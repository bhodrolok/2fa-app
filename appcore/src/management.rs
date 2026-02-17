use crate::{
    client::Client,
    features::{
        codegen::{Algorithm, CodeType},
        otpauth::Account,
    },
    utils,
};

pub const XCHACHA20_KEY_SIZE: usize = 32;
pub const XCHACHA20_NONCE_SIZE: usize = 24;

impl Client {
    pub fn export_accounts_using_seed(&self) -> String {
        let key: [u8; XCHACHA20_KEY_SIZE] = self.seed[0..XCHACHA20_KEY_SIZE].try_into().unwrap();
        let nonce: [u8; XCHACHA20_NONCE_SIZE] = self.seed
            [XCHACHA20_KEY_SIZE..XCHACHA20_KEY_SIZE + XCHACHA20_NONCE_SIZE]
            .try_into()
            .unwrap();

        let db = self.export_accounts_db_002(&key, &nonce);

        serde_json::json!({
            "version": 2,
            "db": db
        })
        .to_string()
    }

    pub fn export_accounts_using_automatic_internal_backups(&self) -> String {
        let key: [u8; XCHACHA20_KEY_SIZE] = self.automatic_internal_backups_pass_hash
            [0..XCHACHA20_KEY_SIZE]
            .try_into()
            .unwrap();
        let nonce: [u8; XCHACHA20_NONCE_SIZE] = self.automatic_internal_backups_pass_hash
            [XCHACHA20_KEY_SIZE..XCHACHA20_KEY_SIZE + XCHACHA20_NONCE_SIZE]
            .try_into()
            .unwrap();

        let db = self.export_accounts_db_002(&key, &nonce);

        serde_json::json!({
            "version": 2,
            "salt": data_encoding::BASE64.encode(&self.automatic_internal_backups_pass_salt),
            "db": db
        })
        .to_string()
    }

    pub fn export_accounts(&self, pass: &str) -> String {
        use sha3::Digest;

        let mut hasher = sha3::Sha3_512::new();
        hasher.update(&self.seed);

        let mut system_random = [0u8; 64];
        getrandom::fill(&mut system_random).unwrap();
        hasher.update(&system_random);

        let hash = hasher.finalize();

        let mut salt = [0u8; 16];
        salt.copy_from_slice(&hash[..16]);

        // m: 256 MiB t: 4 p: 2
        let params = argon2::Params::new(256 * 1024, 4, 2, None).unwrap();
        let argon2 =
            argon2::Argon2::new(argon2::Algorithm::Argon2id, argon2::Version::V0x13, params);

        let mut out = [0u8; XCHACHA20_KEY_SIZE + XCHACHA20_NONCE_SIZE];
        argon2
            .hash_password_into(pass.as_bytes(), &salt, &mut out)
            .unwrap();

        let key: [u8; XCHACHA20_KEY_SIZE] = out[..XCHACHA20_KEY_SIZE].try_into().unwrap();
        let nonce: [u8; XCHACHA20_NONCE_SIZE] = out[XCHACHA20_KEY_SIZE..].try_into().unwrap();

        let db = self.export_accounts_db_002(&key, &nonce);

        serde_json::json!({
            "version": 2,
            "salt": data_encoding::BASE64.encode(&salt),
            "db": db
        })
        .to_string()
    }

    pub fn export_accounts_db_002(
        &self,
        key: &[u8; XCHACHA20_KEY_SIZE],
        nonce: &[u8; XCHACHA20_NONCE_SIZE],
    ) -> String {
        use chacha20poly1305::{KeyInit, aead::AeadMut};

        let mut accounts_array: Vec<serde_json::Value> = Vec::with_capacity(self.accounts.len());

        for account in &self.accounts {
            let mut account_obj = serde_json::Value::Object(serde_json::Map::new());

            account_obj["id"] = data_encoding::HEXLOWER_PERMISSIVE
                .encode(&account.id.to_be_bytes())
                .into();

            if !account.name.is_empty() {
                account_obj["name"] = account.name.clone().into();
            }

            if !account.issuer.is_empty() {
                account_obj["issuer"] = account.issuer.clone().into();
            }

            if !account.note.is_empty() {
                account_obj["note"] = account.note.clone().into();
            }

            account_obj["secret"] = data_encoding::BASE64.encode(&account.secret).into();
            account_obj["code_type"] = match account.code_type {
                CodeType::HOTP => "HOTP".into(),
                CodeType::TOTP => "TOTP".into(),
                CodeType::STEAM => "STEAM".into(),
            };

            if matches!(account.code_type, CodeType::HOTP | CodeType::TOTP) {
                account_obj["algorithm"] = match account.algorithm {
                    Algorithm::SHA1 => "SHA1".into(),
                    Algorithm::SHA256 => "SHA256".into(),
                    Algorithm::SHA512 => "SHA512".into(),
                };

                account_obj["digits"] = account.digits.into();
            }

            if account.code_type == CodeType::HOTP {
                account_obj["counter"] = account.counter.into();
            }

            if account.code_type == CodeType::TOTP {
                account_obj["period"] = account.period.into();
            }

            account_obj["color"] = data_encoding::HEXLOWER_PERMISSIVE
                .encode(&account.color.to_be_bytes())
                .into();

            accounts_array.push(account_obj);
        }

        let plaintext = serde_json::to_string(&serde_json::Value::Array(accounts_array))
            .unwrap_or_else(|_| "[]".to_string());

        let mut cipher = chacha20poly1305::XChaCha20Poly1305::new(key.into());
        let nonce = chacha20poly1305::XNonce::from_slice(nonce);

        let ciphertext = cipher.encrypt(nonce, plaintext.as_bytes()).unwrap();

        data_encoding::BASE64.encode(&ciphertext)
    }

    pub fn import_accounts_using_seed(&mut self, data: &str) -> bool {
        let obj: serde_json::Value = match serde_json::from_str(data) {
            Ok(v) => v,
            Err(_) => return false,
        };

        let db = match obj.get("db").and_then(|v| v.as_str()) {
            Some(s) => s,
            None => return false,
        };

        let key: [u8; XCHACHA20_KEY_SIZE] = self.seed[0..XCHACHA20_KEY_SIZE].try_into().unwrap();
        let nonce: [u8; XCHACHA20_NONCE_SIZE] = self.seed
            [XCHACHA20_KEY_SIZE..XCHACHA20_KEY_SIZE + XCHACHA20_NONCE_SIZE]
            .try_into()
            .unwrap();

        self.import_accounts_db_002(db, &key, &nonce)
    }

    pub fn import_accounts(&mut self, pass: &str, data: &str) -> bool {
        use chacha20poly1305::{KeyInit, aead::AeadMut};

        let obj: serde_json::Value = match serde_json::from_str(data) {
            Ok(v) => v,
            Err(_) => return false,
        };

        let version = match obj.get("version").and_then(|v| v.as_u64()) {
            Some(v) => v,
            None => return false,
        };

        match version {
            2 => {
                let db = match obj.get("db").and_then(|v| v.as_str()) {
                    Some(s) => s,
                    None => return false,
                };

                let salt = match obj.get("salt").and_then(|v| v.as_str()) {
                    Some(s) => match data_encoding::BASE64.decode(s.as_bytes()) {
                        Ok(b) => b,
                        Err(_) => return false,
                    },
                    None => return false,
                };

                // m: 256 MiB t: 4 p: 2
                let params = argon2::Params::new(256 * 1024, 4, 2, None).unwrap();
                let argon2 = argon2::Argon2::new(
                    argon2::Algorithm::Argon2id,
                    argon2::Version::V0x13,
                    params,
                );

                let mut out = [0u8; XCHACHA20_KEY_SIZE + XCHACHA20_NONCE_SIZE];
                if argon2
                    .hash_password_into(pass.as_bytes(), &salt, &mut out)
                    .is_err()
                {
                    return false;
                }

                let key: [u8; XCHACHA20_KEY_SIZE] = match out[..XCHACHA20_KEY_SIZE].try_into() {
                    Ok(k) => k,
                    Err(_) => return false,
                };

                let nonce: [u8; XCHACHA20_NONCE_SIZE] = match out[XCHACHA20_KEY_SIZE..].try_into() {
                    Ok(n) => n,
                    Err(_) => return false,
                };

                self.import_accounts_db_002(db, &key, &nonce)
            }

            1 => {
                const KEY_SIZE: usize = 32;
                const NONCE_SIZE: usize = 12;

                let ciphertext = match obj.get("data").and_then(|v| v.as_str()) {
                    Some(s) => match data_encoding::BASE64.decode(s.as_bytes()) {
                        Ok(b) => b,
                        Err(_) => return false,
                    },
                    None => return false,
                };

                let salt = match obj.get("salt").and_then(|v| v.as_str()) {
                    Some(s) => match data_encoding::BASE64.decode(s.as_bytes()) {
                        Ok(b) => b,
                        Err(_) => return false,
                    },
                    None => return false,
                };

                let argon = match obj.get("argon2id").and_then(|v| v.as_object()) {
                    Some(o) => o,
                    None => return false,
                };

                let m = match argon.get("m").and_then(|v| v.as_u64()) {
                    Some(v) => v,
                    None => return false,
                };

                let t = match argon.get("t").and_then(|v| v.as_u64()) {
                    Some(v) => v,
                    None => return false,
                };

                let p = match argon.get("p").and_then(|v| v.as_u64()) {
                    Some(v) => v,
                    None => return false,
                };

                let params = argon2::Params::new(m as u32, t as u32, p as u32, None).unwrap();
                let argon2 = argon2::Argon2::new(
                    argon2::Algorithm::Argon2id,
                    argon2::Version::V0x13,
                    params,
                );

                let mut out = [0u8; KEY_SIZE + NONCE_SIZE];
                if argon2
                    .hash_password_into(format!("979{}", pass).as_bytes(), &salt, &mut out)
                    .is_err()
                {
                    return false;
                }

                let key = &out[..KEY_SIZE];
                let nonce = &out[KEY_SIZE..];

                let mut cipher = chacha20poly1305::ChaCha20Poly1305::new(key.into());
                let nonce = chacha20poly1305::Nonce::from_slice(nonce);

                let plaintext = match cipher.decrypt(nonce, ciphertext.as_ref()) {
                    Ok(pt) => pt,
                    Err(_) => return false,
                };

                let plaintext = match String::from_utf8(plaintext) {
                    Ok(s) => s,
                    Err(_) => return false,
                };

                self.import_accounts_db_001(&plaintext)
            }

            _ => false,
        }
    }

    pub fn import_accounts_db_002(
        &mut self,
        db: &str,
        key: &[u8; XCHACHA20_KEY_SIZE],
        nonce: &[u8; XCHACHA20_NONCE_SIZE],
    ) -> bool {
        use chacha20poly1305::{KeyInit, aead::AeadMut};

        let ciphertext = match data_encoding::BASE64.decode(db.as_bytes()) {
            Ok(ct) => ct,
            Err(_) => return false,
        };

        let mut cipher = chacha20poly1305::XChaCha20Poly1305::new(key.into());
        let nonce = chacha20poly1305::XNonce::from_slice(nonce);

        let plaintext_bytes = match cipher.decrypt(nonce, ciphertext.as_ref()) {
            Ok(pt) => pt,
            Err(_) => return false,
        };

        let json_array: serde_json::Value = match serde_json::from_slice(&plaintext_bytes) {
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

            let id = match obj.get("id").and_then(|v| v.as_str()) {
                Some(s) => match data_encoding::HEXLOWER_PERMISSIVE.decode(s.as_bytes()) {
                    Ok(bytes) if bytes.len() == 8 => u64::from_be_bytes(bytes.try_into().unwrap()),
                    _ => continue,
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

            let secret = match obj.get("secret").and_then(|v| v.as_str()) {
                Some(s) => match data_encoding::BASE64.decode(s.as_bytes()) {
                    Ok(b) => b,
                    Err(_) => continue,
                },
                None => continue,
            };

            let code_type = match obj.get("code_type").and_then(|v| v.as_str()) {
                Some("HOTP") => CodeType::HOTP,
                Some("TOTP") => CodeType::TOTP,
                Some("STEAM") => CodeType::STEAM,
                _ => continue,
            };

            let algorithm = match obj.get("algorithm").and_then(|v| v.as_str()) {
                Some("SHA1") => Algorithm::SHA1,
                Some("SHA256") => Algorithm::SHA256,
                Some("SHA512") => Algorithm::SHA512,
                _ => Algorithm::SHA1,
            };

            let mut period = obj.get("period").and_then(|v| v.as_u64()).unwrap_or(30);
            if period < 10 {
                continue;
            }

            let mut digits = obj.get("digits").and_then(|v| v.as_u64()).unwrap_or(6) as u32;
            if code_type == CodeType::STEAM {
                period = 30;
                digits = 5;
            }
            if digits < 4 || digits > 10 {
                continue;
            }

            let counter = obj.get("counter").and_then(|v| v.as_u64()).unwrap_or(0);

            let color = obj
                .get("color")
                .and_then(|v| v.as_str())
                .and_then(|s| data_encoding::HEXLOWER_PERMISSIVE.decode(s.as_bytes()).ok())
                .and_then(|b| {
                    if b.len() == 4 {
                        Some(u32::from_be_bytes(b.try_into().unwrap()))
                    } else {
                        None
                    }
                })
                .unwrap_or(utils::colors::GRAY);

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
                color,
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

    pub fn import_accounts_db_001(&mut self, db: &str) -> bool {
        let json_array: serde_json::Value = match serde_json::from_slice(&db.as_bytes()) {
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

            let secret = match obj.get("secret").and_then(|v| v.as_str()) {
                Some(s) => match data_encoding::BASE64.decode(s.as_bytes()) {
                    Ok(b) => b,
                    Err(_) => continue,
                },
                None => continue,
            };

            let code_type = match obj.get("otpType").and_then(|v| v.as_str()) {
                Some("HOTP") => CodeType::HOTP,
                Some("TOTP") => CodeType::TOTP,
                _ => continue,
            };

            let algorithm = match obj.get("otpHashFunction").and_then(|v| v.as_str()) {
                Some("SHA-1") => Algorithm::SHA1,
                Some("SHA-256") => Algorithm::SHA256,
                Some("SHA-512") => Algorithm::SHA512,
                _ => Algorithm::SHA1,
            };

            let digits = obj.get("digits").and_then(|v| v.as_u64()).unwrap_or(6) as u32;
            if digits < 4 || digits > 10 {
                continue;
            }

            let counter = obj.get("counter").and_then(|v| v.as_u64()).unwrap_or(0);

            let period = obj.get("period").and_then(|v| v.as_u64()).unwrap_or(30);
            if period < 10 {
                continue;
            }

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
                color: utils::colors::GRAY,
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
