use aegis::aegis256::Aegis256;
use argon2::{Algorithm, Argon2, Params, Version};
use hkdf::Hkdf;
use sha3::Digest;
use sha3::Sha3_512;

use crate::{
    account::Account,
    features::{management::native::schema::Format, otpauth::serialize::serialize},
};

pub fn export(accounts: &[Account], passphrase: &str) -> String {
    if passphrase.is_empty() {
        let json = Format {
            version: 4,
            salt: None,
            hash: None,
            content: serde_json::json!(accounts.iter().map(serialize).collect::<Vec<String>>()),
        };

        return serde_json::to_string(&json).unwrap();
    }

    let mut salt = [0u8; 16];
    getrandom::fill(&mut salt).unwrap();

    let m_cost = 256 * 1024;
    let t_cost = 4;
    let p_cost = 2;
    let params = Params::new(m_cost, t_cost, p_cost, None).unwrap();
    let argon2id = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    let mut key = [0u8; 64];
    argon2id
        .hash_password_into(passphrase.as_bytes(), &salt, &mut key)
        .unwrap();

    export_encrypted(accounts, &salt, &key)
}

pub fn export_encrypted(accounts: &[Account], salt: &[u8; 16], key: &[u8; 64]) -> String {
    let mut content: Vec<String> = accounts.iter().map(serialize).collect();

    if content.is_empty() {
        let mut junk = [0u8; 64];
        getrandom::fill(&mut junk).unwrap();
        content.push(data_encoding::BASE64.encode(&junk));
    }

    let mut content = serde_json::to_string(&content).unwrap();

    let target = content.len().max(4096).next_multiple_of(4096);
    content.push_str(&" ".repeat(target - content.len()));

    let digest = Sha3_512::digest(content.as_bytes());
    let hash: [u8; 16] = digest[..16].try_into().unwrap();

    let hk = Hkdf::<Sha3_512>::new(Some(&hash), key);
    let mut derived = [0u8; 64];
    hk.expand(
        b"app.ninesevennine.twofactorauthenticator.export.v4",
        &mut derived,
    )
    .unwrap();

    let key: [u8; 32] = derived[..32].try_into().unwrap();
    let nonce: [u8; 32] = derived[32..].try_into().unwrap();

    let salt = data_encoding::BASE64.encode(salt);
    let hash = data_encoding::BASE64.encode(&hash);
    let header = format!("4|{}|{}", salt, hash);
    let aad: [u8; 64] = Sha3_512::digest(header.as_bytes()).into();

    let (mut ciphertext, tag) = Aegis256::<32>::new(&key, &nonce).encrypt(content.as_bytes(), &aad);
    ciphertext.extend_from_slice(&tag);

    let json = Format {
        version: 4,
        salt: Some(salt),
        hash: Some(hash),
        content: serde_json::json!(data_encoding::BASE64.encode(&ciphertext)),
    };

    serde_json::to_string(&json).unwrap()
}
