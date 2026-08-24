use aegis::aegis256::Aegis256;
use argon2::{Argon2, Params, Version};
use hkdf::Hkdf;
use sha3::Sha3_512;

use crate::{
    account::Account,
    features::{
        management::{
            native::v3::schema::{Entry, Format},
            result::ImportResult,
        },
        otp::{algorithm::Algorithm, r#type::CodeType},
    },
};

pub fn import(accounts: &mut Vec<Account>, json: &str, passphrase: &str) -> ImportResult {
    let format: Format = match serde_json::from_str(json) {
        Ok(f) => f,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if format.version != 3 {
        return ImportResult::UnsupportedVersion;
    }

    if passphrase.is_empty() {
        return ImportResult::IncorrectPassphrase;
    }

    let Ok(salt) = data_encoding::BASE64.decode(format.salt.as_bytes()) else {
        return ImportResult::InvalidFormat;
    };

    let passphrase_salt = match salt.len() {
        16 => &salt[..],
        32 => &salt[16..],
        _ => return ImportResult::InvalidFormat,
    };

    let params = Params::new(256 * 1024, 4, 2, None).unwrap();
    let argon2id = Argon2::new(argon2::Algorithm::Argon2id, Version::V0x13, params);

    let mut key = [0u8; 64];
    if argon2id
        .hash_password_into(passphrase.as_bytes(), passphrase_salt, &mut key)
        .is_err()
    {
        return ImportResult::InvalidFormat;
    }

    import_encrypted(accounts, json, &key)
}

pub fn import_encrypted(accounts: &mut Vec<Account>, json: &str, key: &[u8; 64]) -> ImportResult {
    let Ok(format) = serde_json::from_str::<Format>(json) else {
        return ImportResult::InvalidFormat;
    };

    if format.version != 3 {
        return ImportResult::UnsupportedVersion;
    }

    let Ok(salt) = data_encoding::BASE64.decode(format.salt.as_bytes()) else {
        return ImportResult::InvalidFormat;
    };

    let (key, nonce): ([u8; 32], [u8; 32]) = match salt.len() {
        16 => (key[..32].try_into().unwrap(), key[32..].try_into().unwrap()),
        32 => {
            let hk = Hkdf::<Sha3_512>::new(Some(&salt), &key[..]);
            let mut okm = [0u8; 64];
            hk.expand(b"979.2fa.v3", &mut okm).unwrap();
            (okm[..32].try_into().unwrap(), okm[32..].try_into().unwrap())
        }
        _ => return ImportResult::InvalidFormat,
    };

    let Ok(db) = data_encoding::BASE64.decode(format.db.as_bytes()) else {
        return ImportResult::InvalidFormat;
    };

    if db.len() < 16 {
        return ImportResult::InvalidFormat;
    }

    let (ciphertext, tag) = db.split_at(db.len() - 16);
    let tag: [u8; 16] = tag.try_into().unwrap();

    let Ok(content) = Aegis256::<16>::new(&key, &nonce).decrypt(ciphertext, &tag, b"") else {
        return ImportResult::IncorrectPassphrase;
    };

    let Ok(entries) = serde_json::from_slice::<Vec<serde_json::Value>>(&content) else {
        return ImportResult::InvalidFormat;
    };

    for entry in entries {
        let Ok(account) = serde_json::from_value::<Entry>(entry) else {
            continue;
        };

        let Some(id) = data_encoding::HEXLOWER_PERMISSIVE
            .decode(account.id.as_bytes())
            .ok()
            .and_then(|bytes| <[u8; 8]>::try_from(bytes).ok())
            .map(u64::from_be_bytes)
        else {
            continue;
        };

        let Ok(secret) = data_encoding::BASE64.decode(account.secret.as_bytes()) else {
            continue;
        };

        let mut code_type = match account.code_type.as_str() {
            "HOTP" => CodeType::HOTP,
            "TOTP" => CodeType::TOTP,
            "STEAM" => CodeType::STEAM,
            _ => continue,
        };

        let mut algorithm = match account.algorithm.as_deref() {
            Some("SHA256") => Algorithm::SHA256,
            Some("SHA512") => Algorithm::SHA512,
            _ => Algorithm::SHA1,
        };

        let decode_base64_str = |s: Option<String>| -> String {
            s.and_then(|s| data_encoding::BASE64.decode(s.as_bytes()).ok())
                .and_then(|b| String::from_utf8(b).ok())
                .unwrap_or_default()
        };

        let mut name = decode_base64_str(account.name);
        let mut issuer = decode_base64_str(account.issuer);
        let note = decode_base64_str(account.note);

        let mut period = account.period.unwrap_or(30);
        let mut digits = account.digits.unwrap_or(6);

        if code_type == CodeType::STEAM {
            issuer = "Steam".to_string();
            code_type = CodeType::STEAM;
            algorithm = Algorithm::SHA1;
            period = 30;
            digits = 5;
        }

        if name == issuer {
            name.clear();
        }

        if secret.is_empty() || !(4..=10).contains(&digits) || period < 10 {
            continue;
        }

        let counter = account.counter.unwrap_or(0);

        let color = account
            .color
            .and_then(|c| data_encoding::HEXLOWER_PERMISSIVE.decode(c.as_bytes()).ok())
            .and_then(|b| <[u8; 4]>::try_from(b).ok())
            .map(u32::from_be_bytes)
            .unwrap_or(0xFF808080);

        accounts.push(Account {
            id,
            name,
            issuer,
            note,
            secret,
            code_type,
            algorithm,
            digits,
            counter,
            period,
            color,
        });
    }

    ImportResult::Success
}
