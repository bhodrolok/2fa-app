use crate::{
    client::Client,
    features::{
        codegen::{Algorithm, CodeType},
        otpauth::Account,
    },
    utils,
};

impl Client {
    pub fn export_accounts_aegis(&self, pass: &str) -> String {
        use aes_gcm::{Aes256Gcm, KeyInit, aead::Aead};
        use scrypt::Params as ScryptParams;
        use sha3::Digest;

        let mut entries_array: Vec<serde_json::Value> = Vec::with_capacity(self.accounts.len());

        for account in &self.accounts {
            let mut info = serde_json::Value::Object(serde_json::Map::new());

            info["secret"] = data_encoding::BASE32_NOPAD.encode(&account.secret).into();

            info["algo"] = match account.algorithm {
                Algorithm::SHA1 => "SHA1".into(),
                Algorithm::SHA256 => "SHA256".into(),
                Algorithm::SHA512 => "SHA512".into(),
            };

            let mut digits = account.digits;
            if account.code_type == CodeType::STEAM {
                digits = 5;
            }

            info["digits"] = digits.into();
            info["counter"] = account.counter.into();
            info["period"] = account.period.into();

            let entry = serde_json::json!({
                "type": match account.code_type {
                    CodeType::TOTP => "totp",
                    CodeType::HOTP => "hotp",
                    CodeType::STEAM => "steam",
                },
                "uuid": utils::convert::id_to_uuid(account.id),
                "name": account.name,
                "issuer": account.issuer,
                "note": account.note,
                "info": info,
            });

            entries_array.push(entry);
        }

        let db_obj = serde_json::json!({
            "version": 3,
            "entries": serde_json::Value::Array(entries_array),
        });

        let db_plaintext = serde_json::to_string(&db_obj).unwrap();

        let mut master_key = [0u8; 32];
        getrandom::fill(&mut master_key).unwrap();

        let mut db_nonce = [0u8; 12];
        getrandom::fill(&mut db_nonce).unwrap();

        let cipher = Aes256Gcm::new_from_slice(&master_key).unwrap();
        let mut db_ct = cipher
            .encrypt(
                aes_gcm::Nonce::from_slice(&db_nonce),
                db_plaintext.as_bytes(),
            )
            .unwrap();

        let db_tag = db_ct.split_off(db_ct.len() - 16);
        let db_ciphertext = db_ct;

        let mut hasher = sha3::Sha3_512::new();
        hasher.update(&self.seed);

        let mut system_random = [0u8; 64];
        getrandom::fill(&mut system_random).unwrap();
        hasher.update(&system_random);

        let hash = hasher.finalize();
        let mut salt = [0u8; 16];
        salt.copy_from_slice(&hash[..16]);

        const N: u32 = 256 * 1024;
        const R: u32 = 8;
        const P: u32 = 2;

        let params = ScryptParams::new((N as f64).log2() as u8, R, P, 32).unwrap();

        let mut derived_key = [0u8; 32];
        scrypt::scrypt(pass.as_bytes(), &salt, &params, &mut derived_key).unwrap();

        let mut key_nonce = [0u8; 12];
        getrandom::fill(&mut key_nonce).unwrap();

        let key_cipher = Aes256Gcm::new_from_slice(&derived_key).unwrap();
        let mut key_ct = key_cipher
            .encrypt(aes_gcm::Nonce::from_slice(&key_nonce), master_key.as_ref())
            .unwrap();

        let key_tag = key_ct.split_off(key_ct.len() - 16);
        let key_ciphertext = key_ct;

        let slot = serde_json::json!({
            "type": 1,
            "n": N,
            "r": R,
            "p": P,
            "salt": data_encoding::HEXLOWER.encode(&salt),
            "key": data_encoding::HEXLOWER.encode(&key_ciphertext),
            "key_params": {
                "nonce": data_encoding::HEXLOWER.encode(&key_nonce),
                "tag": data_encoding::HEXLOWER.encode(&key_tag),
            }
        });

        let header = serde_json::json!({
            "slots": serde_json::Value::Array(vec![slot]),
            "params": {
                "nonce": data_encoding::HEXLOWER.encode(&db_nonce),
                "tag": data_encoding::HEXLOWER.encode(&db_tag),
            }
        });

        serde_json::json!({
            "version": 1,
            "header": header,
            "db": data_encoding::BASE64.encode(&db_ciphertext),
        })
        .to_string()
    }

    pub fn import_accounts_aegis(&mut self, pass: &str, data: &str) -> bool {
        use aes_gcm::{Aes256Gcm, KeyInit, aead::Aead};

        let obj: serde_json::Value = match serde_json::from_str(data) {
            Ok(v) => v,
            Err(_) => return false,
        };

        let version = match obj.get("version").and_then(|v| v.as_u64()) {
            Some(v) => v,
            None => return false,
        };

        if version != 1 {
            return false;
        }

        let header = match obj.get("header").and_then(|v| v.as_object()) {
            Some(h) => h,
            None => return false,
        };

        let slots = match header.get("slots").and_then(|v| v.as_array()) {
            Some(s) => s,
            None => return false,
        };

        let params = match header.get("params").and_then(|v| v.as_object()) {
            Some(p) => p,
            None => return false,
        };

        let db_nonce = match params.get("nonce").and_then(|v| v.as_str()) {
            Some(s) => match data_encoding::HEXLOWER_PERMISSIVE.decode(s.as_bytes()) {
                Ok(b) => b,
                Err(_) => return false,
            },
            None => return false,
        };

        let db_tag = match params.get("tag").and_then(|v| v.as_str()) {
            Some(s) => match data_encoding::HEXLOWER_PERMISSIVE.decode(s.as_bytes()) {
                Ok(b) => b,
                Err(_) => return false,
            },
            None => return false,
        };

        let db_encrypted = match obj.get("db").and_then(|v| v.as_str()) {
            Some(s) => match data_encoding::BASE64.decode(s.as_bytes()) {
                Ok(b) => b,
                Err(_) => return false,
            },
            None => return false,
        };

        let mut master_key: Option<Vec<u8>> = None;

        for slot in slots {
            let slot_obj = match slot.as_object() {
                Some(o) => o,
                None => continue,
            };

            let slot_type = match slot_obj.get("type").and_then(|v| v.as_u64()) {
                Some(t) => t,
                None => continue,
            };

            if slot_type != 1 {
                continue;
            }

            let n = match slot_obj.get("n").and_then(|v| v.as_u64()) {
                Some(v) => v as u32,
                None => continue,
            };

            let r = match slot_obj.get("r").and_then(|v| v.as_u64()) {
                Some(v) => v as u32,
                None => continue,
            };

            let p = match slot_obj.get("p").and_then(|v| v.as_u64()) {
                Some(v) => v as u32,
                None => continue,
            };

            let salt = match slot_obj.get("salt").and_then(|v| v.as_str()) {
                Some(s) => match data_encoding::HEXLOWER_PERMISSIVE.decode(s.as_bytes()) {
                    Ok(b) => b,
                    Err(_) => continue,
                },
                None => continue,
            };

            let key_params = match slot_obj.get("key_params").and_then(|v| v.as_object()) {
                Some(kp) => kp,
                None => continue,
            };

            let key_nonce = match key_params.get("nonce").and_then(|v| v.as_str()) {
                Some(s) => match data_encoding::HEXLOWER_PERMISSIVE.decode(s.as_bytes()) {
                    Ok(b) => b,
                    Err(_) => continue,
                },
                None => continue,
            };

            let key_tag = match key_params.get("tag").and_then(|v| v.as_str()) {
                Some(s) => match data_encoding::HEXLOWER_PERMISSIVE.decode(s.as_bytes()) {
                    Ok(b) => b,
                    Err(_) => continue,
                },
                None => continue,
            };

            let key_encrypted = match slot_obj.get("key").and_then(|v| v.as_str()) {
                Some(s) => match data_encoding::HEXLOWER_PERMISSIVE.decode(s.as_bytes()) {
                    Ok(b) => b,
                    Err(_) => continue,
                },
                None => continue,
            };

            let params = match scrypt::Params::new((n as f64).log2() as u8, r, p, 32) {
                Ok(p) => p,
                Err(_) => continue,
            };

            let mut derived_key = [0u8; 32];
            if scrypt::scrypt(pass.as_bytes(), &salt, &params, &mut derived_key).is_err() {
                continue;
            }

            let mut ciphertext_with_tag = Vec::with_capacity(key_encrypted.len() + key_tag.len());
            ciphertext_with_tag.extend_from_slice(&key_encrypted);
            ciphertext_with_tag.extend_from_slice(&key_tag);

            let cipher = Aes256Gcm::new_from_slice(&derived_key).unwrap();
            let nonce = aes_gcm::Nonce::from_slice(&key_nonce);

            let decrypted = match cipher.decrypt(nonce, ciphertext_with_tag.as_ref()) {
                Ok(d) => d,
                Err(_) => continue,
            };

            master_key = Some(decrypted);
            break;
        }

        let master_key = match master_key {
            Some(k) => k,
            None => return false,
        };

        let mut ciphertext_with_tag = Vec::with_capacity(db_encrypted.len() + db_tag.len());
        ciphertext_with_tag.extend_from_slice(&db_encrypted);
        ciphertext_with_tag.extend_from_slice(&db_tag);

        let cipher = match Aes256Gcm::new_from_slice(&master_key) {
            Ok(c) => c,
            Err(_) => return false,
        };

        let nonce = aes_gcm::Nonce::from_slice(&db_nonce);

        let decrypted = match cipher.decrypt(nonce, ciphertext_with_tag.as_ref()) {
            Ok(d) => d,
            Err(_) => return false,
        };

        let db_str = match String::from_utf8(decrypted) {
            Ok(s) => s,
            Err(_) => return false,
        };

        let db_obj: serde_json::Value = match serde_json::from_str(&db_str) {
            Ok(v) => v,
            Err(_) => return false,
        };

        let db_version = match db_obj.get("version").and_then(|v| v.as_u64()) {
            Some(v) => v,
            None => return false,
        };

        if db_version != 3 {
            return false;
        }

        let entries = match db_obj.get("entries").and_then(|v| v.as_array()) {
            Some(e) => e,
            None => return false,
        };

        let entries_str = match serde_json::to_string(entries) {
            Ok(s) => s,
            Err(_) => return false,
        };

        self.import_accounts_aegis_entries_003(&entries_str)
    }

    pub fn import_accounts_aegis_entries_003(&mut self, entries: &str) -> bool {
        let json_array: serde_json::Value = match serde_json::from_slice(&entries.as_bytes()) {
            Ok(val) => val,
            Err(_) => return false,
        };

        let json_array = match json_array.as_array() {
            Some(arr) => arr,
            None => return false,
        };

        let mut new_accounts: Vec<Account> = Vec::with_capacity(json_array.len());

        for item in json_array {
            let obj = match item.as_object() {
                Some(o) => o,
                None => continue,
            };

            let code_type = match obj.get("type").and_then(|v| v.as_str()) {
                Some("totp") => CodeType::TOTP,
                Some("hotp") => CodeType::HOTP,
                Some("steam") => CodeType::STEAM,
                _ => continue,
            };

            let id = match obj.get("uuid").and_then(|v| v.as_str()) {
                Some(uuid) => match utils::convert::uuid_to_id(uuid) {
                    Some(id) => id,
                    None => continue,
                },
                None => continue,
            };

            let name = obj
                .get("name")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string();

            let issuer = obj
                .get("issuer")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string();

            let note = obj
                .get("note")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string();

            let info = match obj.get("info").and_then(|v| v.as_object()) {
                Some(i) => i,
                None => continue,
            };

            let secret = match info.get("secret").and_then(|v| v.as_str()) {
                Some(s) => match data_encoding::BASE32_NOPAD.decode(s.as_bytes()) {
                    Ok(b) => b,
                    Err(_) => continue,
                },
                None => continue,
            };

            let algorithm = match info.get("algo").and_then(|v| v.as_str()) {
                Some("SHA1") => Algorithm::SHA1,
                Some("SHA256") => Algorithm::SHA256,
                Some("SHA512") => Algorithm::SHA512,
                _ => Algorithm::SHA1,
            };

            let mut period = info.get("period").and_then(|v| v.as_u64()).unwrap_or(30);
            if period < 10 {
                continue;
            }

            let mut digits = info.get("digits").and_then(|v| v.as_u64()).unwrap_or(6) as u32;
            if code_type == CodeType::STEAM {
                period = 30;
                digits = 5;
            }
            if digits < 4 || digits > 10 {
                continue;
            }

            let counter = info.get("counter").and_then(|v| v.as_u64()).unwrap_or(0);

            let account = Account {
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
                color: utils::colors::WHITE,
            };

            new_accounts.push(account);
        }

        for imported in new_accounts {
            if let Some(existing) = self.accounts.iter_mut().find(|a| a.id == imported.id) {
                *existing = imported;
            } else {
                self.accounts.push(imported);
            }
        }

        true
    }
}
