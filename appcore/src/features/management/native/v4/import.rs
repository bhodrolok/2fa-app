use aegis::aegis256::Aegis256;
use argon2::{Algorithm, Argon2, Params, Version};
use hkdf::Hkdf;
use sha3::Digest;
use sha3::Sha3_512;
use zeroize::Zeroizing;

use crate::features::management::native::v4::schema::AEGIS256_AAD_LEN;
use crate::features::management::native::v4::schema::AEGIS256_KEY_LEN;
use crate::features::management::native::v4::schema::AEGIS256_NONCE_LEN;
use crate::features::management::native::v4::schema::AEGIS256_TAG_LEN;
use crate::features::management::native::v4::schema::ARGON2ID_M_COST;
use crate::features::management::native::v4::schema::ARGON2ID_P_COST;
use crate::features::management::native::v4::schema::ARGON2ID_T_COST;
use crate::features::management::native::v4::schema::DERIVED_LEN;
use crate::features::management::native::v4::schema::Format;
use crate::features::management::native::v4::schema::HASH_LEN;
use crate::features::management::native::v4::schema::KEY_LEN;
use crate::features::management::native::v4::schema::OKM;
use crate::features::management::native::v4::schema::SALT_LEN;
use crate::features::management::native::v4::schema::VERSION;
use crate::{
    account::Account,
    features::{management::result::ImportResult, otpauth::deserialize::deserialize},
};

pub fn import(json: &str, passphrase: &str, out_accounts: &mut Vec<Account>) -> ImportResult {
    let format: Format = match serde_json::from_str(json) {
        Ok(f) => f,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if format.version != VERSION {
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
                out_accounts.push(account);
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
        .and_then(|salt| <[u8; SALT_LEN]>::try_from(salt).ok())
    else {
        return ImportResult::InvalidFormat;
    };

    let params = Params::new(ARGON2ID_M_COST, ARGON2ID_T_COST, ARGON2ID_P_COST, None).unwrap();
    let argon2id = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    let mut key = Zeroizing::new([0u8; KEY_LEN]);
    if argon2id
        .hash_password_into(passphrase.as_bytes(), &salt, key.as_mut_slice())
        .is_err()
    {
        return ImportResult::InvalidFormat;
    }

    import_encrypted(json, &key, out_accounts)
}

pub fn import_encrypted(
    json: &str,
    key: &[u8; KEY_LEN],
    out_accounts: &mut Vec<Account>,
) -> ImportResult {
    let format: Format = match serde_json::from_str(json) {
        Ok(f) => f,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if format.version != VERSION {
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

    if content.len() < AEGIS256_TAG_LEN {
        return ImportResult::InvalidFormat;
    }

    let (ciphertext, tag) = content.split_at(content.len() - AEGIS256_TAG_LEN);
    let tag: [u8; AEGIS256_TAG_LEN] = tag.try_into().unwrap();

    let Some(digest) = data_encoding::BASE64
        .decode(hash.as_bytes())
        .ok()
        .and_then(|hash| <[u8; HASH_LEN]>::try_from(hash).ok())
    else {
        return ImportResult::InvalidFormat;
    };

    let hk = Hkdf::<Sha3_512>::new(Some(&digest), key);
    let mut derived = Zeroizing::new([0u8; DERIVED_LEN]);
    hk.expand(OKM, derived.as_mut_slice()).unwrap();

    let key: [u8; AEGIS256_KEY_LEN] = derived[..DERIVED_LEN].try_into().unwrap();
    let nonce: [u8; AEGIS256_NONCE_LEN] = derived[DERIVED_LEN..].try_into().unwrap();
    let header = format!("{}|{}|{}", VERSION, salt, hash);
    let aad: [u8; AEGIS256_AAD_LEN] = Sha3_512::digest(header.as_bytes()).into();

    let Ok(content) =
        Aegis256::<AEGIS256_TAG_LEN>::new(&key, &nonce).decrypt(ciphertext, &tag, &aad)
    else {
        return ImportResult::IncorrectPassphrase;
    };
    let content = Zeroizing::new(content);

    if Sha3_512::digest(&content)[..HASH_LEN] != digest[..] {
        return ImportResult::InvalidFormat;
    }

    let Ok(uris) = serde_json::from_slice::<Vec<String>>(&content) else {
        return ImportResult::InvalidFormat;
    };
    let uris = Zeroizing::new(uris);

    for uri in uris.iter() {
        if let Some(account) = deserialize(uri, false) {
            out_accounts.push(account);
        }
    }

    ImportResult::Success
}
