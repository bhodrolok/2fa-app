use aegis::aegis256::Aegis256;
use argon2::{Algorithm, Argon2, Params, Version};
use crypto_common::getrandom;
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
use crate::{account::Account, features::otpauth::serialize::serialize};

pub fn export(
    accounts: &[Account],
    passphrase: &str,
    out_salt: &mut [u8; SALT_LEN],
    out_key: &mut [u8; KEY_LEN],
) -> String {
    if passphrase.is_empty() {
        let json = Format {
            version: VERSION,
            salt: None,
            hash: None,
            content: serde_json::json!(accounts.iter().map(serialize).collect::<Vec<String>>()),
        };

        return serde_json::to_string(&json).unwrap();
    }

    getrandom::fill(out_salt).unwrap();

    let params = Params::new(ARGON2ID_M_COST, ARGON2ID_T_COST, ARGON2ID_P_COST, None).unwrap();
    let argon2id = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    argon2id
        .hash_password_into(passphrase.as_bytes(), out_salt, out_key)
        .unwrap();

    export_encrypted(accounts, out_salt, out_key)
}

pub fn export_encrypted(
    accounts: &[Account],
    salt: &[u8; SALT_LEN],
    key: &[u8; KEY_LEN],
) -> String {
    let mut content: Zeroizing<Vec<String>> =
        Zeroizing::new(accounts.iter().map(serialize).collect());

    if content.is_empty() {
        let mut junk = [0u8; 64];
        getrandom::fill(&mut junk).unwrap();
        content.push(data_encoding::BASE64.encode(&junk));
    }

    let mut content = Zeroizing::new(serde_json::to_string(&*content).unwrap());

    let target = content.len().max(4096).next_multiple_of(4096);
    let padding = " ".repeat(target - content.len());
    content.push_str(&padding);

    let digest = Sha3_512::digest(content.as_bytes());
    let hash: [u8; HASH_LEN] = digest[..HASH_LEN].try_into().unwrap();

    let hk = Hkdf::<Sha3_512>::new(Some(&hash), key);
    let mut derived = Zeroizing::new([0u8; DERIVED_LEN]);
    hk.expand(OKM, derived.as_mut_slice()).unwrap();

    let key =
        Zeroizing::new(<[u8; AEGIS256_KEY_LEN]>::try_from(&derived[..AEGIS256_KEY_LEN]).unwrap());
    let nonce =
        Zeroizing::new(<[u8; AEGIS256_NONCE_LEN]>::try_from(&derived[AEGIS256_KEY_LEN..]).unwrap());

    let salt = data_encoding::BASE64.encode(salt);
    let hash = data_encoding::BASE64.encode(&hash);
    let header = format!("{}|{}|{}", VERSION, salt, hash);
    let aad: [u8; AEGIS256_AAD_LEN] = Sha3_512::digest(header.as_bytes()).into();

    let (mut ciphertext, tag) =
        Aegis256::<AEGIS256_TAG_LEN>::new(&key, &nonce).encrypt(content.as_bytes(), &aad);
    ciphertext.extend_from_slice(&tag);

    let json = Format {
        version: VERSION,
        salt: Some(salt),
        hash: Some(hash),
        content: serde_json::json!(data_encoding::BASE64.encode(&ciphertext)),
    };

    serde_json::to_string(&json).unwrap()
}
