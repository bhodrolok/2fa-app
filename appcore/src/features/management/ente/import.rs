use aes_gcm::aead::Aead;
use argon2::Argon2;
use chacha20poly1305::{Key, XChaCha20Poly1305, XNonce};
use hmac::KeyInit;

use crate::features::{
    management::{ente::schema::Format, result::ImportResult},
    otpauth::{self, Account},
};

// This shit doesn't work even though I'm certain is 100% correct. Fails due to incorrect passphrase.
pub fn v1(accounts: &mut Vec<Account>, json: &str, passphrase: &str) -> ImportResult {
    let format: Format = match serde_json::from_str(json) {
        Ok(f) => f,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if format.version != 1 {
        return ImportResult::UnsupportedVersion;
    }

    if passphrase.is_empty() {
        return ImportResult::IncorrectPassphrase;
    }

    let salt = match data_encoding::BASE64.decode(format.kdf_params.salt.as_bytes()) {
        Ok(s) => s,
        Err(_) => return ImportResult::InvalidFormat,
    };

    let encryption_nonce = match data_encoding::BASE64.decode(format.encryption_nonce.as_bytes()) {
        Ok(s) => s,
        Err(_) => return ImportResult::InvalidFormat,
    };

    let encrypted_data = match data_encoding::BASE64.decode(format.encrypted_data.as_bytes()) {
        Ok(c) => c,
        Err(_) => return ImportResult::InvalidFormat,
    };

    let mem_limit_kib = format.kdf_params.mem_limit / 1024;

    let argon_params = match argon2::ParamsBuilder::new()
        .m_cost(mem_limit_kib)
        .t_cost(format.kdf_params.ops_limit)
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

    let key: &Key = match derived_key.as_slice().try_into() {
        Ok(k) => k,
        Err(_) => return ImportResult::InvalidFormat,
    };

    let nonce: &XNonce = match encryption_nonce.as_slice().try_into() {
        Ok(n) => n,
        Err(_) => return ImportResult::InvalidFormat,
    };

    let cipher = XChaCha20Poly1305::new(key);
    let decrypted_bytes = match cipher.decrypt(nonce, encrypted_data.as_ref()) {
        Ok(b) => b,
        Err(_) => return ImportResult::IncorrectPassphrase,
    };

    let data = match std::str::from_utf8(&decrypted_bytes) {
        Ok(s) => s,
        Err(_) => return ImportResult::InvalidFormat,
    };

    for line in data.lines() {
        let line = line.trim();

        if line.is_empty() {
            continue;
        }

        if let Some(account) = otpauth::parse(line) {
            accounts.push(account);
        }
    }

    ImportResult::Success
}
