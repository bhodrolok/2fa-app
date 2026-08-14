use aes_gcm::{
    Aes256Gcm, Key,
    aead::{Aead, Nonce},
};
use hmac::KeyInit;
use serde_json::Value;

use crate::{
    features::{
        management::aegis::schema::{Database, Entry, EntryInfo, Header, Params, Slot, Vault},
        old_otpauth::Account,
    },
    utils::{
        convert::u64_to_uuid,
        types::{Algorithm, CodeType},
    },
};

pub fn export(accounts: &Vec<Account>, passphrase: &str) -> String {
    if passphrase.is_empty() {
        return String::new();
    }

    let mut entries = Vec::with_capacity(accounts.len());

    for account in accounts {
        let (entry_type, period, counter) = match account.code_type {
            CodeType::TOTP => ("totp".to_string(), Some(account.period), None),
            CodeType::STEAM => ("steam".to_string(), Some(account.period), None),
            CodeType::HOTP => ("hotp".to_string(), None, Some(account.counter)),
        };

        let algo = match account.algorithm {
            Algorithm::SHA1 => "SHA1".to_string(),
            Algorithm::SHA256 => "SHA256".to_string(),
            Algorithm::SHA512 => "SHA512".to_string(),
        };

        entries.push(Entry {
            entry_type,
            uuid: u64_to_uuid(account.id),
            name: account.name.clone(),
            issuer: account.issuer.clone(),
            note: account.note.clone(),
            favorite: false,
            icon: None,
            icon_mime: None,
            icon_hash: None,
            groups: Vec::new(),
            info: EntryInfo {
                secret: data_encoding::BASE32_NOPAD.encode(&account.secret),
                algo,
                digits: account.digits,
                period,
                counter,
            },
        });
    }

    let db = Database {
        version: 3,
        entries,
        groups: Vec::new(),
    };

    let db_plaintext = match serde_json::to_vec(&db) {
        Ok(bytes) => bytes,
        Err(_) => return String::new(),
    };

    let mut salt = [0u8; 32];
    let mut master_key = [0u8; 32];
    let mut slot_nonce_bytes = [0u8; 12];
    let mut db_nonce_bytes = [0u8; 12];

    if getrandom::fill(&mut salt).is_err()
        || getrandom::fill(&mut master_key).is_err()
        || getrandom::fill(&mut slot_nonce_bytes).is_err()
        || getrandom::fill(&mut db_nonce_bytes).is_err()
    {
        return String::new();
    }

    let slot_uuid = match getrandom::u64() {
        Ok(id) => u64_to_uuid(id),
        Err(_) => return String::new(),
    };

    let scrypt_params = match scrypt::Params::new(15, 8, 1) {
        Ok(p) => p,
        Err(_) => return String::new(),
    };

    let mut derived_key = [0u8; 32];
    if scrypt::scrypt(
        passphrase.as_bytes(),
        &salt,
        &scrypt_params,
        &mut derived_key,
    )
    .is_err()
    {
        return String::new();
    }

    let slot_key: &Key<Aes256Gcm> = match derived_key.as_slice().try_into() {
        Ok(k) => k,
        Err(_) => return String::new(),
    };

    let slot_nonce: &Nonce<Aes256Gcm> = match slot_nonce_bytes.as_slice().try_into() {
        Ok(n) => n,
        Err(_) => return String::new(),
    };

    let slot_cipher = Aes256Gcm::new(slot_key);
    let mut slot_sealed = match slot_cipher.encrypt(slot_nonce, master_key.as_slice()) {
        Ok(c) => c,
        Err(_) => return String::new(),
    };

    if slot_sealed.len() < 16 {
        return String::new();
    }

    let slot_split = slot_sealed.len() - 16;
    let slot_tag = slot_sealed.split_off(slot_split);

    let db_key: &Key<Aes256Gcm> = match master_key.as_slice().try_into() {
        Ok(k) => k,
        Err(_) => return String::new(),
    };

    let db_nonce: &Nonce<Aes256Gcm> = match db_nonce_bytes.as_slice().try_into() {
        Ok(n) => n,
        Err(_) => return String::new(),
    };

    let db_cipher = Aes256Gcm::new(db_key);
    let mut db_sealed = match db_cipher.encrypt(db_nonce, db_plaintext.as_slice()) {
        Ok(c) => c,
        Err(_) => return String::new(),
    };

    if db_sealed.len() < 16 {
        return String::new();
    }

    let db_split = db_sealed.len() - 16;
    let db_tag = db_sealed.split_off(db_split);

    let vault = Vault {
        version: 1,
        header: Header {
            slots: Some(vec![Slot {
                slot_type: 1,
                uuid: slot_uuid,
                key: data_encoding::HEXLOWER.encode(&slot_sealed),
                key_params: Params {
                    nonce: data_encoding::HEXLOWER.encode(&slot_nonce_bytes),
                    tag: data_encoding::HEXLOWER.encode(&slot_tag),
                },
                n: 32768,
                r: 8,
                p: 1,
                salt: data_encoding::HEXLOWER.encode(&salt),
                repaired: true,
                is_backup: false,
            }]),
            params: Some(Params {
                nonce: data_encoding::HEXLOWER.encode(&db_nonce_bytes),
                tag: data_encoding::HEXLOWER.encode(&db_tag),
            }),
        },
        db: Value::String(data_encoding::BASE64.encode(&db_sealed)),
    };

    serde_json::to_string(&vault).unwrap_or_default()
}
