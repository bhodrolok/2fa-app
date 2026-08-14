use aes_gcm::{
    Aes256Gcm, Key,
    aead::{Aead, Nonce},
};
use hmac::KeyInit;
use pbkdf2::pbkdf2_hmac;
use sha2::Sha256;

use crate::features::{
    management::{
        _2fas::schema::{Format, Service},
        result::ImportResult,
    },
    old_otpauth::{self, Account},
};

pub fn import(accouts: &mut Vec<Account>, json: &str, passphrase: &str) -> ImportResult {
    let format: Format = match serde_json::from_str(json) {
        Ok(f) => f,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if format.schema_version != 4 {
        return ImportResult::UnsupportedVersion;
    }

    let services: Vec<Service> = match format.services {
        Some(s) if !s.is_empty() => s,
        _ => {
            let services_encrypted = match &format.services_encrypted {
                Some(s) => s,
                None => return ImportResult::InvalidFormat,
            };

            let parts: Vec<&str> = services_encrypted.split(':').collect();
            if parts.len() != 3 {
                return ImportResult::InvalidFormat;
            }

            let ciphertext = match data_encoding::BASE64.decode(parts[0].as_bytes()) {
                Ok(c) => c,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let salt = match data_encoding::BASE64.decode(parts[1].as_bytes()) {
                Ok(s) => s,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let iv = match data_encoding::BASE64.decode(parts[2].as_bytes()) {
                Ok(i) => i,
                Err(_) => return ImportResult::InvalidFormat,
            };

            if iv.len() != 12 || ciphertext.len() < 16 {
                return ImportResult::InvalidFormat;
            }

            if passphrase.is_empty() {
                return ImportResult::IncorrectPassphrase;
            }

            let mut derived_key = [0u8; 32];
            pbkdf2_hmac::<Sha256>(passphrase.as_bytes(), &salt, 10_000, &mut derived_key);

            let key: &Key<Aes256Gcm> = match derived_key.as_slice().try_into() {
                Ok(k) => k,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let nonce: &Nonce<Aes256Gcm> = match iv.as_slice().try_into() {
                Ok(n) => n,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let cipher = Aes256Gcm::new(key);
            let decrypted_bytes = match cipher.decrypt(nonce, ciphertext.as_ref()) {
                Ok(b) => b,
                Err(_) => return ImportResult::IncorrectPassphrase,
            };

            let decrypted_services: Vec<Service> = match serde_json::from_slice(&decrypted_bytes) {
                Ok(s) => s,
                Err(_) => return ImportResult::InvalidFormat,
            };

            decrypted_services
        }
    };

    for service in services {
        let Some(link) = &service.otp.link else {
            continue;
        };

        if let Some(mut account) = old_otpauth::parse(link) {
            account.color = 0xFFFFFFFF;
            accouts.push(account);
        }
    }

    ImportResult::Success
}
