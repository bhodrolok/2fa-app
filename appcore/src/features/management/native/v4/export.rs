use aegis::aegis256::Aegis256;
use argon2::{Algorithm, Argon2, Params, Version};
use hkdf::Hkdf;
use sha3::Digest;
use sha3::Sha3_512;
use zeroize::Zeroizing;

use crate::features::management::native::v4::schema::Format;
use crate::{account::Account, features::otpauth::serialize::serialize};

pub fn export(
    accounts: &[Account],
    passphrase: &str,
    out_salt: &mut [u8; 16],
    out_key: &mut [u8; 64],
) -> String {
    if passphrase.is_empty() {
        let json = Format {
            version: 4,
            salt: None,
            hash: None,
            content: serde_json::json!(accounts.iter().map(serialize).collect::<Vec<String>>()),
        };

        return serde_json::to_string(&json).unwrap();
    }

    getrandom::fill(out_salt).unwrap();

    let m_cost = 256 * 1024;
    let t_cost = 4;
    let p_cost = 2;
    let params = Params::new(m_cost, t_cost, p_cost, None).unwrap();
    let argon2id = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    argon2id
        .hash_password_into(passphrase.as_bytes(), out_salt, out_key)
        .unwrap();

    export_encrypted(accounts, out_salt, out_key)
}

pub fn export_encrypted(accounts: &[Account], salt: &[u8; 16], key: &[u8; 64]) -> String {
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
    let hash: [u8; 16] = digest[..16].try_into().unwrap();

    let hk = Hkdf::<Sha3_512>::new(Some(&hash), key);
    let mut derived = Zeroizing::new([0u8; 64]);
    hk.expand(
        b"app.ninesevennine.twofactorauthenticator.export.v4",
        derived.as_mut_slice(),
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
