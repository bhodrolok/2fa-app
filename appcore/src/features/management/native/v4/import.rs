use aegis::aegis256::Aegis256;
use argon2::{Algorithm, Argon2, Params, Version};
use hkdf::Hkdf;
use sha3::Digest;
use sha3::Sha3_512;
use zeroize::Zeroizing;

use crate::features::management::native::v4::schema::Format;
use crate::{
    account::Account,
    features::{management::result::ImportResult, otpauth::deserialize::deserialize},
};

pub fn import(accounts: &mut Vec<Account>, json: &str, passphrase: &str) -> ImportResult {
    let format: Format = match serde_json::from_str(json) {
        Ok(f) => f,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if format.version != 4 {
        return ImportResult::UnsupportedVersion;
    }

    if format.salt.is_none() && format.hash.is_none() {
        let Some(content) = format.content.as_array() else {
            return ImportResult::InvalidFormat;
        };

        for uri in content {
            let Some(uri) = uri.as_str() else {
                return ImportResult::InvalidFormat;
            };

            if let Some(account) = deserialize(uri, false) {
                accounts.push(account);
            }
        }

        return ImportResult::Success;
    }

    if passphrase.is_empty() {
        return ImportResult::IncorrectPassphrase;
    }

    let Some(salt) = format
        .salt
        .as_ref()
        .and_then(|salt| data_encoding::BASE64.decode(salt.as_bytes()).ok())
        .and_then(|salt| <[u8; 16]>::try_from(salt).ok())
    else {
        return ImportResult::InvalidFormat;
    };

    let m_cost = 256 * 1024;
    let t_cost = 4;
    let p_cost = 2;
    let params = Params::new(m_cost, t_cost, p_cost, None).unwrap();
    let argon2id = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    let mut key = Zeroizing::new([0u8; 64]);
    if argon2id
        .hash_password_into(passphrase.as_bytes(), &salt, key.as_mut_slice())
        .is_err()
    {
        return ImportResult::InvalidFormat;
    }

    import_encrypted(accounts, json, &key)
}

pub fn import_encrypted(accounts: &mut Vec<Account>, json: &str, key: &[u8; 64]) -> ImportResult {
    let format: Format = match serde_json::from_str(json) {
        Ok(f) => f,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if format.version != 4 {
        return ImportResult::UnsupportedVersion;
    }

    let (Some(salt), Some(hash)) = (&format.salt, &format.hash) else {
        return ImportResult::InvalidFormat;
    };

    let Some(content) = format
        .content
        .as_str()
        .and_then(|content| data_encoding::BASE64.decode(content.as_bytes()).ok())
    else {
        return ImportResult::InvalidFormat;
    };

    if content.len() < 32 {
        return ImportResult::InvalidFormat;
    }

    let (ciphertext, tag) = content.split_at(content.len() - 32);
    let tag: [u8; 32] = tag.try_into().unwrap();

    let Some(digest) = data_encoding::BASE64
        .decode(hash.as_bytes())
        .ok()
        .and_then(|hash| <[u8; 16]>::try_from(hash).ok())
    else {
        return ImportResult::InvalidFormat;
    };

    let hk = Hkdf::<Sha3_512>::new(Some(&digest), key);
    let mut derived = Zeroizing::new([0u8; 64]);
    hk.expand(
        b"app.ninesevennine.twofactorauthenticator.export.v4",
        derived.as_mut_slice(),
    )
    .unwrap();

    let key: [u8; 32] = derived[..32].try_into().unwrap();
    let nonce: [u8; 32] = derived[32..].try_into().unwrap();
    let header = format!("4|{}|{}", salt, hash);
    let aad: [u8; 64] = Sha3_512::digest(header.as_bytes()).into();

    let Ok(content) = Aegis256::<32>::new(&key, &nonce).decrypt(ciphertext, &tag, &aad) else {
        return ImportResult::IncorrectPassphrase;
    };
    let content = Zeroizing::new(content);

    if Sha3_512::digest(&content)[..16] != digest[..] {
        return ImportResult::InvalidFormat;
    }

    let Ok(uris) = serde_json::from_slice::<Vec<String>>(&content) else {
        return ImportResult::InvalidFormat;
    };
    let uris = Zeroizing::new(uris);

    for uri in uris.iter() {
        if let Some(account) = deserialize(uri, false) {
            accounts.push(account);
        }
    }

    ImportResult::Success
}
