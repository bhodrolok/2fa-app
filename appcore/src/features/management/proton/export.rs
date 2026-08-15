use aes_gcm::{
    Aes256Gcm, Key,
    aead::{Aead, Nonce, Payload},
};
use argon2::Argon2;
use hmac::KeyInit;

use crate::{
    account::Account,
    features::{
        management::proton::schema::{Content, Entry, Format},
        otp::otptype::CodeType,
        otpauth::serialize::serialize,
    },
    utils::convert::u64_to_uuid,
};

pub fn export(accounts: &[Account], passphrase: &str) -> String {
    if passphrase.is_empty() {
        return String::new();
    }

    let mut entries = Vec::with_capacity(accounts.len());

    for account in accounts {
        let entry_type = match account.code_type {
            CodeType::HOTP => continue,
            CodeType::STEAM => "Steam",
            CodeType::TOTP => "Totp",
        };

        entries.push(Entry {
            id: u64_to_uuid(account.id),
            content: Content {
                uri: serialize(account),
                entry_type: entry_type.to_string(),
                name: account.name.clone(),
            },
            note: if account.note.is_empty() {
                None
            } else {
                Some(account.note.clone())
            },
        });
    }

    let plain = Format {
        version: 1,
        entries: Some(entries),
        salt: None,
        content: None,
    };

    let plaintext = match serde_json::to_vec(&plain) {
        Ok(b) => b,
        Err(_) => return String::new(),
    };

    let mut salt = [0u8; 16];
    let mut nonce_bytes = [0u8; 12];

    if getrandom::fill(&mut salt).is_err() || getrandom::fill(&mut nonce_bytes).is_err() {
        return String::new();
    }

    let argon_params = match argon2::ParamsBuilder::new()
        .m_cost(19 * 1024)
        .t_cost(2)
        .p_cost(1)
        .build()
    {
        Ok(p) => p,
        Err(_) => return String::new(),
    };

    let argon2 = Argon2::new(
        argon2::Algorithm::Argon2id,
        argon2::Version::V0x13,
        argon_params,
    );

    let mut derived_key = [0u8; 32];
    if argon2
        .hash_password_into(passphrase.as_bytes(), &salt, &mut derived_key)
        .is_err()
    {
        return String::new();
    }

    let key: &Key<Aes256Gcm> = match derived_key.as_slice().try_into() {
        Ok(k) => k,
        Err(_) => return String::new(),
    };

    let nonce: &Nonce<Aes256Gcm> = match nonce_bytes.as_slice().try_into() {
        Ok(n) => n,
        Err(_) => return String::new(),
    };

    let cipher = Aes256Gcm::new(key);
    let payload = Payload {
        msg: &plaintext,
        aad: b"proton.authenticator.export.v1",
    };

    let ciphertext = match cipher.encrypt(nonce, payload) {
        Ok(c) => c,
        Err(_) => return String::new(),
    };

    let mut content = Vec::with_capacity(nonce_bytes.len() + ciphertext.len());
    content.extend_from_slice(&nonce_bytes);
    content.extend_from_slice(&ciphertext);

    let encrypted = Format {
        version: 1,
        entries: None,
        salt: Some(data_encoding::BASE64.encode(&salt)),
        content: Some(data_encoding::BASE64.encode(&content)),
    };

    serde_json::to_string(&encrypted).unwrap_or_default()
}
