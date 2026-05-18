use crate::{
    client::Client,
    features::otpauth::Account,
    utils::{
        self,
        types::{Algorithm, CodeType},
    },
};

impl Client {
    pub fn export_accounts_aegis(&self, passphrase: &str) -> String {
        use aes_gcm::{Aes256Gcm, KeyInit, aead::Aead};
        use scrypt::Params as ScryptParams;

        let mut entries_array: Vec<serde_json::Value> = Vec::with_capacity(self.accounts.len());

        for account in &self.accounts {
            let digits = if account.code_type == CodeType::STEAM {
                5
            } else {
                account.digits
            };

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
                "info": {
                    "secret": data_encoding::BASE32_NOPAD.encode(&account.secret),
                    "algo": match account.algorithm {
                        Algorithm::SHA1 => "SHA1",
                        Algorithm::SHA256 => "SHA256",
                        Algorithm::SHA512 => "SHA512",
                    },
                    "digits": digits,
                    "counter": account.counter,
                    "period": account.period,
                },
            });

            entries_array.push(entry);
        }

        let db_obj = serde_json::json!({
            "version": 3,
            "entries": entries_array,
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

        let mut salt = [0u8; 16];
        getrandom::fill(&mut salt).unwrap();

        const N: u32 = 256 * 1024;
        const R: u32 = 8;
        const P: u32 = 2;

        let params = ScryptParams::new((N as f64).log2() as u8, R, P).unwrap();

        let mut derived_key = [0u8; 32];
        scrypt::scrypt(passphrase.as_bytes(), &salt, &params, &mut derived_key).unwrap();

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
            "slots": vec![slot],
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

    pub fn import_accounts_aegis(&mut self, passphrase: &str, data: &str) -> bool {
        use aes_gcm::{Aes256Gcm, KeyInit, aead::Aead};

        let Ok(obj) = serde_json::from_str::<serde_json::Value>(data) else {
            return false;
        };

        let Some(version) = obj.get("version").and_then(|v| v.as_u64()) else {
            return false;
        };
        if version != 1 {
            return false;
        }

        let Some(header) = obj.get("header").and_then(|v| v.as_object()) else {
            return false;
        };
        let Some(slots) = header.get("slots").and_then(|v| v.as_array()) else {
            return false;
        };
        let Some(params) = header.get("params").and_then(|v| v.as_object()) else {
            return false;
        };

        let Some(db_nonce_str) = params.get("nonce").and_then(|v| v.as_str()) else {
            return false;
        };
        let Ok(db_nonce) = data_encoding::HEXLOWER_PERMISSIVE.decode(db_nonce_str.as_bytes())
        else {
            return false;
        };

        let Some(db_tag_str) = params.get("tag").and_then(|v| v.as_str()) else {
            return false;
        };
        let Ok(db_tag) = data_encoding::HEXLOWER_PERMISSIVE.decode(db_tag_str.as_bytes()) else {
            return false;
        };

        let Some(db_enc_str) = obj.get("db").and_then(|v| v.as_str()) else {
            return false;
        };
        let Ok(db_encrypted) = data_encoding::BASE64.decode(db_enc_str.as_bytes()) else {
            return false;
        };

        let mut master_key: Option<Vec<u8>> = None;

        for slot in slots {
            let Some(slot_obj) = slot.as_object() else {
                continue;
            };
            let Some(slot_type) = slot_obj.get("type").and_then(|v| v.as_u64()) else {
                continue;
            };
            if slot_type != 1 {
                continue;
            }

            let Some(n) = slot_obj.get("n").and_then(|v| v.as_u64()).map(|v| v as u32) else {
                continue;
            };
            let Some(r) = slot_obj.get("r").and_then(|v| v.as_u64()).map(|v| v as u32) else {
                continue;
            };
            let Some(p) = slot_obj.get("p").and_then(|v| v.as_u64()).map(|v| v as u32) else {
                continue;
            };

            let Some(salt_str) = slot_obj.get("salt").and_then(|v| v.as_str()) else {
                continue;
            };
            let Ok(salt) = data_encoding::HEXLOWER_PERMISSIVE.decode(salt_str.as_bytes()) else {
                continue;
            };

            let Some(key_params) = slot_obj.get("key_params").and_then(|v| v.as_object()) else {
                continue;
            };

            let Some(key_nonce_str) = key_params.get("nonce").and_then(|v| v.as_str()) else {
                continue;
            };
            let Ok(key_nonce) = data_encoding::HEXLOWER_PERMISSIVE.decode(key_nonce_str.as_bytes())
            else {
                continue;
            };

            let Some(key_tag_str) = key_params.get("tag").and_then(|v| v.as_str()) else {
                continue;
            };
            let Ok(key_tag) = data_encoding::HEXLOWER_PERMISSIVE.decode(key_tag_str.as_bytes())
            else {
                continue;
            };

            let Some(key_enc_str) = slot_obj.get("key").and_then(|v| v.as_str()) else {
                continue;
            };
            let Ok(key_encrypted) =
                data_encoding::HEXLOWER_PERMISSIVE.decode(key_enc_str.as_bytes())
            else {
                continue;
            };

            let Ok(params) = scrypt::Params::new((n as f64).log2() as u8, r, p) else {
                continue;
            };

            let mut derived_key = [0u8; 32];
            if scrypt::scrypt(passphrase.as_bytes(), &salt, &params, &mut derived_key).is_err() {
                continue;
            }

            let mut ciphertext_with_tag = Vec::with_capacity(key_encrypted.len() + key_tag.len());
            ciphertext_with_tag.extend_from_slice(&key_encrypted);
            ciphertext_with_tag.extend_from_slice(&key_tag);

            let Ok(cipher) = Aes256Gcm::new_from_slice(&derived_key) else {
                continue;
            };
            let nonce = aes_gcm::Nonce::from_slice(&key_nonce);

            if let Ok(decrypted) = cipher.decrypt(nonce, ciphertext_with_tag.as_ref()) {
                master_key = Some(decrypted);
                break;
            }
        }

        let Some(master_key) = master_key else {
            return false;
        };

        let mut ciphertext_with_tag = Vec::with_capacity(db_encrypted.len() + db_tag.len());
        ciphertext_with_tag.extend_from_slice(&db_encrypted);
        ciphertext_with_tag.extend_from_slice(&db_tag);

        let Ok(cipher) = Aes256Gcm::new_from_slice(&master_key) else {
            return false;
        };
        let nonce = aes_gcm::Nonce::from_slice(&db_nonce);

        let Ok(decrypted) = cipher.decrypt(nonce, ciphertext_with_tag.as_ref()) else {
            return false;
        };
        let Ok(db_str) = String::from_utf8(decrypted) else {
            return false;
        };
        let Ok(db_obj) = serde_json::from_str::<serde_json::Value>(&db_str) else {
            return false;
        };

        let Some(db_version) = db_obj.get("version").and_then(|v| v.as_u64()) else {
            return false;
        };
        if db_version != 3 {
            return false;
        }

        let Some(entries) = db_obj.get("entries").and_then(|v| v.as_array()) else {
            return false;
        };
        let Ok(entries_str) = serde_json::to_string(entries) else {
            return false;
        };

        self.import_accounts_aegis_entries_003(&entries_str)
    }

    pub fn import_accounts_aegis_entries_003(&mut self, entries: &str) -> bool {
        let Ok(json_array) = serde_json::from_slice::<serde_json::Value>(entries.as_bytes()) else {
            return false;
        };
        let Some(json_array) = json_array.as_array() else {
            return false;
        };

        let mut new_accounts: Vec<Account> = Vec::with_capacity(json_array.len());

        for item in json_array {
            let Some(obj) = item.as_object() else {
                continue;
            };

            let code_type = match obj.get("type").and_then(|v| v.as_str()) {
                Some("totp") => CodeType::TOTP,
                Some("hotp") => CodeType::HOTP,
                Some("steam") => CodeType::STEAM,
                _ => continue,
            };

            let Some(id_str) = obj.get("uuid").and_then(|v| v.as_str()) else {
                continue;
            };
            let Some(id) = utils::convert::uuid_to_id(id_str) else {
                continue;
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

            let Some(info) = obj.get("info").and_then(|v| v.as_object()) else {
                continue;
            };

            let Some(secret_str) = info.get("secret").and_then(|v| v.as_str()) else {
                continue;
            };
            let Ok(secret) = data_encoding::BASE32_NOPAD.decode(secret_str.as_bytes()) else {
                continue;
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
            if !(4..=10).contains(&digits) {
                continue;
            }

            let counter = info.get("counter").and_then(|v| v.as_u64()).unwrap_or(0);

            new_accounts.push(Account {
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
            });
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
