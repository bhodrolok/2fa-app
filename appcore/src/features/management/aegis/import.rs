use aes_gcm::{
    Aes256Gcm, Key, KeyInit,
    aead::{Aead, Nonce},
};

use crate::{
    features::{
        management::{
            aegis::schema::{Database, Vault},
            result::ImportResult,
        },
        old_otpauth::Account,
    },
    utils::{
        convert::uuid_to_id,
        types::{Algorithm, CodeType},
    },
};

pub fn v3(accounts: &mut Vec<Account>, json: &str, passphrase: &str) -> ImportResult {
    let vault: Vault = match serde_json::from_str(json) {
        Ok(v) => v,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if vault.version != 1 {
        return ImportResult::UnsupportedVersion;
    }

    let db: Database = match &vault.header.slots {
        Some(slots) if !slots.is_empty() => {
            if passphrase.is_empty() {
                return ImportResult::IncorrectPassphrase;
            }

            let slot = match slots.iter().find(|s| s.slot_type == 1) {
                Some(s) => s,
                None => return ImportResult::InvalidFormat,
            };

            let salt = match data_encoding::HEXLOWER_PERMISSIVE.decode(slot.salt.as_bytes()) {
                Ok(s) => s,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let scrypt_params = match scrypt::Params::new(slot.n.ilog2() as u8, slot.r, slot.p) {
                Ok(p) => p,
                Err(_) => return ImportResult::InvalidFormat,
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
                return ImportResult::InvalidFormat;
            }

            let slot_nonce_bytes =
                match data_encoding::HEXLOWER_PERMISSIVE.decode(slot.key_params.nonce.as_bytes()) {
                    Ok(n) => n,
                    Err(_) => return ImportResult::InvalidFormat,
                };

            let mut slot_ciphertext =
                match data_encoding::HEXLOWER_PERMISSIVE.decode(slot.key.as_bytes()) {
                    Ok(c) => c,
                    Err(_) => return ImportResult::InvalidFormat,
                };

            let slot_tag_bytes =
                match data_encoding::HEXLOWER_PERMISSIVE.decode(slot.key_params.tag.as_bytes()) {
                    Ok(t) => t,
                    Err(_) => return ImportResult::InvalidFormat,
                };

            slot_ciphertext.extend_from_slice(&slot_tag_bytes);

            let slot_key: &Key<Aes256Gcm> = match derived_key.as_slice().try_into() {
                Ok(k) => k,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let slot_nonce: &Nonce<Aes256Gcm> = match slot_nonce_bytes.as_slice().try_into() {
                Ok(n) => n,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let slot_cipher = Aes256Gcm::new(slot_key);
            let master_key = match slot_cipher.decrypt(slot_nonce, slot_ciphertext.as_ref()) {
                Ok(k) => k,
                Err(_) => return ImportResult::IncorrectPassphrase,
            };

            let header_params = match &vault.header.params {
                Some(p) => p,
                None => return ImportResult::InvalidFormat,
            };

            let base64_payload = match vault.db.as_str() {
                Some(s) => s,
                None => return ImportResult::InvalidFormat,
            };

            let db_nonce_bytes =
                match data_encoding::HEXLOWER_PERMISSIVE.decode(header_params.nonce.as_bytes()) {
                    Ok(n) => n,
                    Err(_) => return ImportResult::InvalidFormat,
                };

            let mut db_ciphertext = match data_encoding::BASE64.decode(base64_payload.as_bytes()) {
                Ok(c) => c,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let db_tag_bytes =
                match data_encoding::HEXLOWER_PERMISSIVE.decode(header_params.tag.as_bytes()) {
                    Ok(t) => t,
                    Err(_) => return ImportResult::InvalidFormat,
                };

            db_ciphertext.extend_from_slice(&db_tag_bytes);

            let db_key: &Key<Aes256Gcm> = match master_key.as_slice().try_into() {
                Ok(k) => k,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let db_nonce: &Nonce<Aes256Gcm> = match db_nonce_bytes.as_slice().try_into() {
                Ok(n) => n,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let db_cipher = Aes256Gcm::new(db_key);
            let decrypted_bytes = match db_cipher.decrypt(db_nonce, db_ciphertext.as_ref()) {
                Ok(b) => b,
                Err(_) => return ImportResult::IncorrectPassphrase,
            };

            match serde_json::from_slice(&decrypted_bytes) {
                Ok(plain_db) => plain_db,
                Err(_) => return ImportResult::InvalidFormat,
            }
        }
        _ => match serde_json::from_value(vault.db) {
            Ok(plain) => plain,
            Err(_) => return ImportResult::InvalidFormat,
        },
    };

    if db.version != 3 {
        return ImportResult::UnsupportedVersion;
    }

    for entry in db.entries {
        let secret = match data_encoding::BASE32_NOPAD.decode(entry.info.secret.as_bytes()) {
            Ok(s) => s,
            Err(_) => continue,
        };

        let code_type = match entry.entry_type.as_str() {
            "totp" => CodeType::TOTP,
            "hotp" => CodeType::HOTP,
            "steam" => CodeType::STEAM,
            _ => continue,
        };

        let algorithm = match entry.info.algo.to_uppercase().as_str() {
            "SHA1" => Algorithm::SHA1,
            "SHA256" => Algorithm::SHA256,
            "SHA512" => Algorithm::SHA512,
            _ => continue,
        };

        let id = match uuid_to_id(&entry.uuid) {
            Some(id) => id,
            None => continue,
        };

        let period = entry.info.period.unwrap_or(30) as u64;
        if period < 10 {
            continue;
        }

        let digits = match (code_type, entry.info.digits) {
            (CodeType::STEAM, _) => 5,
            (_, 4..=10) => entry.info.digits,
            _ => continue,
        };

        accounts.push(Account {
            id,
            name: entry.name,
            issuer: entry.issuer,
            note: entry.note,
            secret,
            code_type,
            algorithm,
            counter: entry.info.counter.unwrap_or(0),
            period: period,
            digits: digits,
            color: 0xFFFFFFFF,
        });
    }

    ImportResult::Success
}
