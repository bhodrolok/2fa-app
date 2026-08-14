use aes_gcm::{
    Aes256Gcm, Key,
    aead::{Aead, Nonce, Payload},
};
use argon2::Argon2;
use hmac::KeyInit;

use crate::{
    features::{
        management::{
            proton::schema::{Entry, Format},
            result::ImportResult,
        },
        old_otpauth::{self, Account},
    },
    utils::convert::uuid_to_u64,
};

pub fn import(accounts: &mut Vec<Account>, json: &str, passphrase: &str) -> ImportResult {
    let format: Format = match serde_json::from_str(json) {
        Ok(f) => f,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if format.version != 1 {
        return ImportResult::UnsupportedVersion;
    }

    let entries: Vec<Entry> = match format.entries {
        Some(e) => e,
        _ => {
            let salt_b64 = match &format.salt {
                Some(s) => s,
                None => return ImportResult::InvalidFormat,
            };

            let content_b64 = match &format.content {
                Some(c) => c,
                None => return ImportResult::InvalidFormat,
            };

            let salt = match data_encoding::BASE64.decode(salt_b64.as_bytes()) {
                Ok(s) => s,
                Err(_) => return ImportResult::InvalidFormat,
            };

            if salt.len() != 16 {
                return ImportResult::InvalidFormat;
            }

            let content = match data_encoding::BASE64.decode(content_b64.as_bytes()) {
                Ok(d) => d,
                Err(_) => return ImportResult::InvalidFormat,
            };

            if content.len() <= 28 {
                return ImportResult::InvalidFormat;
            }

            if passphrase.is_empty() {
                return ImportResult::IncorrectPassphrase;
            }

            let (nonce_bytes, ciphertext) = content.split_at(12);
            let nonce: &Nonce<Aes256Gcm> = match nonce_bytes.try_into() {
                Ok(n) => n,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let argon_params = match argon2::ParamsBuilder::new()
                .m_cost(19 * 1024)
                .t_cost(2)
                .p_cost(1)
                .build()
            {
                Ok(p) => p,
                Err(_) => return ImportResult::InvalidFormat,
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
                return ImportResult::InvalidFormat;
            }

            let key: &Key<Aes256Gcm> = match derived_key.as_slice().try_into() {
                Ok(k) => k,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let cipher = Aes256Gcm::new(key);
            let payload = Payload {
                msg: ciphertext,
                aad: b"proton.authenticator.export.v1",
            };

            let decrypted_bytes = match cipher.decrypt(nonce, payload) {
                Ok(b) => b,
                Err(_) => return ImportResult::IncorrectPassphrase,
            };

            let decrypted_format: Format = match serde_json::from_slice(&decrypted_bytes) {
                Ok(f) => f,
                Err(_) => return ImportResult::InvalidFormat,
            };

            if decrypted_format.version != 1 {
                return ImportResult::UnsupportedVersion;
            }

            match decrypted_format.entries {
                Some(e) => e,
                None => return ImportResult::InvalidFormat,
            }
        }
    };

    for entry in entries {
        if let Some(mut account) = old_otpauth::parse(&entry.content.uri) {
            if let Some(id) = uuid_to_u64(&entry.id) {
                account.id = id;
                account.name = entry.content.name;
                account.note = entry.note.unwrap_or_default();
                account.color = 0xFFFFFFFF;
                accounts.push(account);
            }
        }
    }

    ImportResult::Success
}
