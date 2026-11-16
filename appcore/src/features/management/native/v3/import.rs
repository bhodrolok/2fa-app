use aegis::aegis256::Aegis256;
use argon2::{Argon2, Params, Version};
use hkdf::Hkdf;
use sha3::Sha3_512;
use zeroize::Zeroizing;

use crate::{
    account::Account,
    features::{
        management::{
            native::v3::schema::{
                AEGIS256_AAD_LEN, AEGIS256_KEY_LEN, AEGIS256_NONCE_LEN, AEGIS256_TAG_LEN,
                ARGON2ID_M_COST, ARGON2ID_P_COST, ARGON2ID_T_COST, Entry, Format, KEY_LEN,
                SALT_LEN, VERSION,
            },
            result::ImportResult,
        },
        otp::{algorithm::Algorithm, r#type::CodeType},
    },
};

pub fn import(
    json: &str,
    passphrase: &str,
    out_salt: &mut [u8; SALT_LEN],
    out_key: &mut [u8; KEY_LEN],
    out_accounts: &mut Vec<Account>,
) -> ImportResult {
    let format: Format = match serde_json::from_str(json) {
        Ok(f) => f,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if format.version != VERSION {
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

    let params = Params::new(ARGON2ID_M_COST, ARGON2ID_T_COST, ARGON2ID_P_COST, None).unwrap();
    let argon2id = Argon2::new(argon2::Algorithm::Argon2id, Version::V0x13, params);

    let mut key = Zeroizing::new([0u8; KEY_LEN]);
    if argon2id
        .hash_password_into(passphrase.as_bytes(), passphrase_salt, key.as_mut_slice())
        .is_err()
    {
        return ImportResult::InvalidFormat;
    }

    out_salt.copy_from_slice(passphrase_salt);
    *out_key = *key;

    import_encrypted(json, &key, out_salt, out_accounts)
}

pub fn import_encrypted(
    json: &str,
    key: &[u8; KEY_LEN],
    out_salt: &mut [u8; SALT_LEN],
    out_accounts: &mut Vec<Account>,
) -> ImportResult {
    let Ok(format) = serde_json::from_str::<Format>(json) else {
        return ImportResult::InvalidFormat;
    };

    if format.version != VERSION {
        return ImportResult::UnsupportedVersion;
    }

    let Ok(salt) = data_encoding::BASE64.decode(format.salt.as_bytes()) else {
        return ImportResult::InvalidFormat;
    };

    let passphrase_salt = match salt.len() {
        16 => &salt[..],
        32 => &salt[16..],
        _ => return ImportResult::InvalidFormat,
    };

    let (key, nonce): (
        Zeroizing<[u8; AEGIS256_KEY_LEN]>,
        Zeroizing<[u8; AEGIS256_NONCE_LEN]>,
    ) = match salt.len() {
        16 => (
            Zeroizing::new(key[..32].try_into().unwrap()),
            Zeroizing::new(key[32..].try_into().unwrap()),
        ),
        32 => {
            let hk = Hkdf::<Sha3_512>::new(Some(&salt), &key[..]);
            let mut okm = Zeroizing::new([0u8; AEGIS256_AAD_LEN]);
            hk.expand(b"979.2fa.v3", okm.as_mut_slice()).unwrap();
            (
                Zeroizing::new(okm[..AEGIS256_KEY_LEN].try_into().unwrap()),
                Zeroizing::new(okm[AEGIS256_KEY_LEN..].try_into().unwrap()),
            )
        }
        _ => return ImportResult::InvalidFormat,
    };

    let Ok(db) = data_encoding::BASE64.decode(format.db.as_bytes()) else {
        return ImportResult::InvalidFormat;
    };

    if db.len() < AEGIS256_TAG_LEN {
        return ImportResult::InvalidFormat;
    }

    let (ciphertext, tag) = db.split_at(db.len() - AEGIS256_TAG_LEN);
    let tag: [u8; AEGIS256_TAG_LEN] = tag.try_into().unwrap();

    let Ok(content) =
        Aegis256::<AEGIS256_TAG_LEN>::new(&key, &nonce).decrypt(ciphertext, &tag, b"")
    else {
        return ImportResult::IncorrectPassphrase;
    };
    let content = Zeroizing::new(content);

    let Ok(entries) = serde_json::from_slice::<Vec<Entry>>(&content) else {
        return ImportResult::InvalidFormat;
    };
    let entries = Zeroizing::new(entries);

    out_salt.copy_from_slice(passphrase_salt);

    for account in entries.iter() {
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

        let mut name = decode_base64_str(account.name.clone());
        let mut issuer = decode_base64_str(account.issuer.clone());
        let note = decode_base64_str(account.note.clone());

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
            .clone()
            .and_then(|c| data_encoding::HEXLOWER_PERMISSIVE.decode(c.as_bytes()).ok())
            .and_then(|b| <[u8; 4]>::try_from(b).ok())
            .map(u32::from_be_bytes)
            .unwrap_or(0xFF808080);

        out_accounts.push(Account {
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
