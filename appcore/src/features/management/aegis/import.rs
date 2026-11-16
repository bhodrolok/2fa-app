use aes_gcm::{
    Aes256Gcm, Key, KeyInit,
    aead::{Aead, Nonce},
};
use zeroize::{Zeroize, Zeroizing};

use crate::{
    CLIENT,
    account::Account,
    features::{
        management::{
            aegis::schema::{AEGIS_SCRYPT_MAX_N_TIMES_R, AEGIS_SCRYPT_MAX_P, Database, Vault},
            result::ImportResult,
        },
        otp::{algorithm::Algorithm, r#type::CodeType},
    },
    utils::convert::uuid_to_u64,
};

pub fn import(accounts: &mut Vec<Account>, json: &str, passphrase: &str) -> ImportResult {
    let vault: Vault = match serde_json::from_str(json) {
        Ok(v) => v,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if vault.version != 1 {
        return ImportResult::UnsupportedVersion;
    }

    let db: Database = match &vault.header.slots {
        Some(slots) if !slots.is_empty() => {
            if passphrase.is_empty() {
                return ImportResult::IncorrectPassphrase;
            }

            let slot = match slots.iter().find(|s| s.slot_type == 1) {
                Some(s) => s,
                None => return ImportResult::InvalidFormat,
            };

            let salt = match data_encoding::HEXLOWER_PERMISSIVE.decode(slot.salt.as_bytes()) {
                Ok(s) => s,
                Err(_) => return ImportResult::InvalidFormat,
            };

            if !slot.n.is_power_of_two()
                || slot.r == 0
                || u64::from(slot.n) * u64::from(slot.r) > AEGIS_SCRYPT_MAX_N_TIMES_R
                || !(1..=AEGIS_SCRYPT_MAX_P).contains(&slot.p)
            {
                return ImportResult::InvalidFormat;
            }

            let scrypt_params = match scrypt::Params::new(slot.n.ilog2() as u8, slot.r, slot.p) {
                Ok(p) => p,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let mut derived_key = Zeroizing::new([0u8; 32]);
            if scrypt::scrypt(
                passphrase.as_bytes(),
                &salt,
                &scrypt_params,
                derived_key.as_mut_slice(),
            )
            .is_err()
            {
                return ImportResult::InvalidFormat;
            }

            let slot_nonce_bytes =
                match data_encoding::HEXLOWER_PERMISSIVE.decode(slot.key_params.nonce.as_bytes()) {
                    Ok(n) => n,
                    Err(_) => return ImportResult::InvalidFormat,
                };

            let mut slot_ciphertext =
                match data_encoding::HEXLOWER_PERMISSIVE.decode(slot.key.as_bytes()) {
                    Ok(c) => c,
                    Err(_) => return ImportResult::InvalidFormat,
                };

            let slot_tag_bytes =
                match data_encoding::HEXLOWER_PERMISSIVE.decode(slot.key_params.tag.as_bytes()) {
                    Ok(t) => t,
                    Err(_) => return ImportResult::InvalidFormat,
                };

            slot_ciphertext.extend_from_slice(&slot_tag_bytes);

            let slot_key: &Key<Aes256Gcm> = match derived_key.as_slice().try_into() {
                Ok(k) => k,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let slot_nonce: &Nonce<Aes256Gcm> = match slot_nonce_bytes.as_slice().try_into() {
                Ok(n) => n,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let slot_cipher = Aes256Gcm::new(slot_key);
            let master_key = Zeroizing::new(
                match slot_cipher.decrypt(slot_nonce, slot_ciphertext.as_ref()) {
                    Ok(k) => k,
                    Err(_) => return ImportResult::IncorrectPassphrase,
                },
            );

            let header_params = match &vault.header.params {
                Some(p) => p,
                None => return ImportResult::InvalidFormat,
            };

            let base64_payload = match vault.db.as_str() {
                Some(s) => s,
                None => return ImportResult::InvalidFormat,
            };

            let db_nonce_bytes =
                match data_encoding::HEXLOWER_PERMISSIVE.decode(header_params.nonce.as_bytes()) {
                    Ok(n) => n,
                    Err(_) => return ImportResult::InvalidFormat,
                };

            let mut db_ciphertext = match data_encoding::BASE64.decode(base64_payload.as_bytes()) {
                Ok(c) => c,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let db_tag_bytes =
                match data_encoding::HEXLOWER_PERMISSIVE.decode(header_params.tag.as_bytes()) {
                    Ok(t) => t,
                    Err(_) => return ImportResult::InvalidFormat,
                };

            db_ciphertext.extend_from_slice(&db_tag_bytes);

            let db_key: &Key<Aes256Gcm> = match master_key.as_slice().try_into() {
                Ok(k) => k,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let db_nonce: &Nonce<Aes256Gcm> = match db_nonce_bytes.as_slice().try_into() {
                Ok(n) => n,
                Err(_) => return ImportResult::InvalidFormat,
            };

            let db_cipher = Aes256Gcm::new(db_key);
            let decrypted_bytes =
                Zeroizing::new(match db_cipher.decrypt(db_nonce, db_ciphertext.as_ref()) {
                    Ok(b) => b,
                    Err(_) => return ImportResult::IncorrectPassphrase,
                });

            match serde_json::from_slice(&decrypted_bytes) {
                Ok(plain_db) => plain_db,
                Err(_) => return ImportResult::InvalidFormat,
            }
        }
        _ => match serde_json::from_value(vault.db) {
            Ok(plain) => plain,
            Err(_) => return ImportResult::InvalidFormat,
        },
    };

    let db = Zeroizing::new(db);

    if db.version != 3 {
        return ImportResult::UnsupportedVersion;
    }

    for entry in db.entries.iter() {
        let mut secret = match data_encoding::BASE32_NOPAD.decode(entry.info.secret.as_bytes()) {
            Ok(s) => s,
            Err(_) => continue,
        };

        let code_type = match entry.entry_type.as_str() {
            "totp" => CodeType::TOTP,
            "hotp" => CodeType::HOTP,
            "steam" => CodeType::STEAM,
            _ => continue,
        };

        let mut algorithm = match entry.info.algo.to_uppercase().as_str() {
            "SHA1" => Algorithm::SHA1,
            "SHA256" => Algorithm::SHA256,
            "SHA512" => Algorithm::SHA512,
            _ => continue,
        };

        let id = match uuid_to_u64(&entry.uuid) {
            Some(id) => id,
            None => continue,
        };

        let mut issuer = entry.issuer.clone();
        let name = if entry.name == issuer {
            String::new()
        } else {
            entry.name.clone()
        };

        let mut period = entry.info.period.unwrap_or(30) as u64;
        let mut digits = entry.info.digits;

        if code_type == CodeType::TOTP && issuer.eq_ignore_ascii_case("Steam") {
            issuer = "_Steam".to_string();
        }

        if code_type == CodeType::STEAM {
            issuer = "Steam".to_string();
            algorithm = Algorithm::SHA1;
            period = 30;
            digits = 5;
        }

        if secret.is_empty() || !(4..=10).contains(&digits) || period < 10 {
            secret.zeroize();
            continue;
        }

        accounts.push(Account {
            id,
            name,
            color: {
                let client = CLIENT.lock().unwrap();

                match client.map.get(&issuer.to_lowercase()) {
                    Some((_, _, color)) => *color,
                    None => 0xFF808080,
                }
            },
            issuer,
            note: entry.note.clone(),
            secret,
            code_type,
            algorithm,
            counter: entry.info.counter.unwrap_or(0),
            period: period,
            digits: digits,
        });
    }

    ImportResult::Success
}
