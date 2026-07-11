use crate::{
    client::Client,
    features::otpauth::Account,
    utils::{
        self,
        types::{Algorithm, CodeType},
    },
};

pub const XCHACHA20_KEY_SIZE: usize = 32;
pub const XCHACHA20_NONCE_SIZE: usize = 24;
pub const XCHACHA20_KEY_NONCE_SIZE: usize = XCHACHA20_KEY_SIZE + XCHACHA20_NONCE_SIZE;

pub const AEGIS256_TAG_LEN: usize = 16;
pub const AEGIS256_KEY_LEN: usize = 32;
pub const AEGIS256_NONCE_LEN: usize = 32;
pub const AEGIS256_KEY_NONCE_LEN: usize = AEGIS256_KEY_LEN + AEGIS256_NONCE_LEN;

pub const IMPORT_ERROR_NO_ACCOUNTS: i32 = 0;
pub const IMPORT_ERROR_INCORRECT_PASSPHRASE: i32 = -1;
pub const IMPORT_ERROR_INCORRECT_VERSION: i32 = -2;
pub const IMPORT_ERROR_INVALID_FILE: i32 = -3;
pub const IMPORT_ERROR_UNKNOWN: i32 = -4;

impl Client {
    pub fn export_accounts_using_unlock_passphrase_hash(&self, passphrase_salt: &[u8]) -> String {
        use hkdf::Hkdf;
        use sha3::Sha3_512;

        let mut hkdf_salt = [0u8; 16];
        getrandom::fill(&mut hkdf_salt).unwrap();

        let mut salt = Vec::with_capacity(hkdf_salt.len() + passphrase_salt.len());
        salt.extend_from_slice(&hkdf_salt);
        salt.extend_from_slice(passphrase_salt);

        let hk = Hkdf::<Sha3_512>::new(Some(&salt), &self.unlock_passphrase_hash);
        let mut okm = [0u8; AEGIS256_KEY_NONCE_LEN];
        hk.expand(b"979.2fa.v3", &mut okm).unwrap();

        let key: [u8; AEGIS256_KEY_LEN] = okm[..AEGIS256_KEY_LEN].try_into().unwrap();
        let nonce: [u8; AEGIS256_NONCE_LEN] = okm[AEGIS256_KEY_LEN..].try_into().unwrap();

        let db = self.export_accounts_db_003(&key, &nonce);
        serde_json::json!({
            "version": 3,
            "salt": data_encoding::BASE64.encode(&salt),
            "db": db
        })
        .to_string()
    }

    pub fn export_accounts(&self, passphrase: &str) -> String {
        let mut salt = [0u8; 16];
        getrandom::fill(&mut salt).unwrap();

        // m: 256 MiB t: 4 p: 2
        let params = argon2::Params::new(256 * 1024, 4, 2, None).unwrap();
        let argon2 =
            argon2::Argon2::new(argon2::Algorithm::Argon2id, argon2::Version::V0x13, params);

        let mut out = [0u8; AEGIS256_KEY_NONCE_LEN];
        argon2
            .hash_password_into(passphrase.as_bytes(), &salt, &mut out)
            .unwrap();

        let key: [u8; AEGIS256_KEY_LEN] = out[..AEGIS256_KEY_LEN].try_into().unwrap();
        let nonce: [u8; AEGIS256_NONCE_LEN] = out[AEGIS256_KEY_LEN..].try_into().unwrap();

        let db = self.export_accounts_db_003(&key, &nonce);

        serde_json::json!({
            "version": 3,
            "salt": data_encoding::BASE64.encode(&salt),
            "db": db
        })
        .to_string()
    }

    pub fn export_accounts_db_003(
        &self,
        key: &[u8; AEGIS256_KEY_LEN],
        nonce: &[u8; AEGIS256_NONCE_LEN],
    ) -> String {
        use aegis::aegis256::Aegis256;

        let mut accounts_array: Vec<serde_json::Value> = Vec::with_capacity(self.accounts.len());

        for account in &self.accounts {
            let mut account_obj = serde_json::Value::Object(serde_json::Map::new());

            account_obj["id"] = data_encoding::HEXLOWER_PERMISSIVE
                .encode(&account.id.to_be_bytes())
                .into();

            if !account.name.is_empty() {
                account_obj["name"] = data_encoding::BASE64.encode(account.name.as_bytes()).into();
            }

            if !account.issuer.is_empty() {
                account_obj["issuer"] = data_encoding::BASE64
                    .encode(account.issuer.as_bytes())
                    .into();
            }

            if !account.note.is_empty() {
                account_obj["note"] = data_encoding::BASE64.encode(account.note.as_bytes()).into();
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

        let (ciphertext, tag) =
            Aegis256::<AEGIS256_TAG_LEN>::new(key, nonce).encrypt(plaintext.as_bytes(), b"");

        let mut output = Vec::with_capacity(ciphertext.len() + tag.len());
        output.extend_from_slice(&ciphertext);
        output.extend_from_slice(&tag);

        data_encoding::BASE64.encode(&output)
    }

    pub fn import_accounts_using_automatic_backups_key_003(
        &mut self,
        passphrase: &str,
        data: &str,
    ) -> i32 {
        use hkdf::Hkdf;
        use sha3::Sha3_512;

        let obj: serde_json::Value = match serde_json::from_str(data) {
            Ok(v) => v,
            Err(_) => return IMPORT_ERROR_INVALID_FILE,
        };

        if obj.get("version").and_then(|v| v.as_u64()) != Some(3) {
            return IMPORT_ERROR_INCORRECT_VERSION;
        }

        let db = match obj.get("db").and_then(|v| v.as_str()) {
            Some(s) => s,
            None => return IMPORT_ERROR_INVALID_FILE,
        };

        let salt = match obj.get("salt").and_then(|v| v.as_str()) {
            Some(s) => match data_encoding::BASE64.decode(s.as_bytes()) {
                Ok(b) => b,
                Err(_) => return IMPORT_ERROR_INVALID_FILE,
            },
            None => return IMPORT_ERROR_INVALID_FILE,
        };

        if salt.len() <= 16 {
            return IMPORT_ERROR_INVALID_FILE;
        }
        let passphrase_salt = &salt[16..];

        // m: 256 MiB t: 4 p: 2
        let params = argon2::Params::new(256 * 1024, 4, 2, None).unwrap();
        let argon2 =
            argon2::Argon2::new(argon2::Algorithm::Argon2id, argon2::Version::V0x13, params);
        let mut automatic_backups_key = [0u8; 64];
        if argon2
            .hash_password_into(
                passphrase.as_bytes(),
                passphrase_salt,
                &mut automatic_backups_key,
            )
            .is_err()
        {
            return IMPORT_ERROR_UNKNOWN;
        }

        let hk = Hkdf::<Sha3_512>::new(Some(&salt), &automatic_backups_key);
        let mut okm = [0u8; AEGIS256_KEY_NONCE_LEN];
        hk.expand(b"979.2fa.v3", &mut okm).unwrap();

        let key: [u8; AEGIS256_KEY_LEN] = okm[..AEGIS256_KEY_LEN].try_into().unwrap();
        let nonce: [u8; AEGIS256_NONCE_LEN] = okm[AEGIS256_KEY_LEN..].try_into().unwrap();

        self.import_accounts_db_003(&key, &nonce, db)
    }

    pub fn import_accounts_using_unlock_passphrase_hash(&mut self, data: &str) -> i32 {
        use hkdf::Hkdf;
        use sha3::Sha3_512;

        let obj: serde_json::Value = match serde_json::from_str(data) {
            Ok(v) => v,
            Err(_) => return IMPORT_ERROR_INVALID_FILE,
        };

        if obj.get("version").and_then(|v| v.as_u64()) != Some(3) {
            return IMPORT_ERROR_INCORRECT_VERSION;
        }

        let db = match obj.get("db").and_then(|v| v.as_str()) {
            Some(s) => s,
            None => return IMPORT_ERROR_INVALID_FILE,
        };

        let salt = match obj.get("salt").and_then(|v| v.as_str()) {
            Some(s) => match data_encoding::BASE64.decode(s.as_bytes()) {
                Ok(b) => b,
                Err(_) => return IMPORT_ERROR_INVALID_FILE,
            },
            None => return IMPORT_ERROR_INVALID_FILE,
        };

        if salt.len() <= 16 {
            return IMPORT_ERROR_INVALID_FILE;
        }

        let hk = Hkdf::<Sha3_512>::new(Some(&salt), &self.unlock_passphrase_hash);
        let mut okm = [0u8; AEGIS256_KEY_NONCE_LEN];
        hk.expand(b"979.2fa.v3", &mut okm).unwrap();

        let key: [u8; AEGIS256_KEY_LEN] = okm[..AEGIS256_KEY_LEN].try_into().unwrap();
        let nonce: [u8; AEGIS256_NONCE_LEN] = okm[AEGIS256_KEY_LEN..].try_into().unwrap();

        self.import_accounts_db_003(&key, &nonce, db)
    }

    pub fn import_accounts(&mut self, passphrase: &str, data: &str) -> i32 {
        let obj: serde_json::Value = match serde_json::from_str(data) {
            Ok(v) => v,
            Err(_) => return IMPORT_ERROR_INVALID_FILE,
        };

        let version = match obj.get("version").and_then(|v| v.as_u64()) {
            Some(v) => v,
            None => return IMPORT_ERROR_INVALID_FILE,
        };

        match version {
            3 => {
                let db = match obj.get("db").and_then(|v| v.as_str()) {
                    Some(s) => s,
                    None => return IMPORT_ERROR_INVALID_FILE,
                };

                let salt = match obj.get("salt").and_then(|v| v.as_str()) {
                    Some(s) => match data_encoding::BASE64.decode(s.as_bytes()) {
                        Ok(b) => b,
                        Err(_) => return IMPORT_ERROR_INVALID_FILE,
                    },
                    None => return IMPORT_ERROR_INVALID_FILE,
                };

                // m: 256 MiB t: 4 p: 2
                let params = argon2::Params::new(256 * 1024, 4, 2, None).unwrap();
                let argon2 = argon2::Argon2::new(
                    argon2::Algorithm::Argon2id,
                    argon2::Version::V0x13,
                    params,
                );

                let mut out = [0u8; AEGIS256_KEY_NONCE_LEN];
                if argon2
                    .hash_password_into(passphrase.as_bytes(), &salt, &mut out)
                    .is_err()
                {
                    return IMPORT_ERROR_UNKNOWN;
                }

                let key: [u8; AEGIS256_KEY_LEN] = out[..AEGIS256_KEY_LEN].try_into().unwrap();
                let nonce: [u8; AEGIS256_NONCE_LEN] = out[AEGIS256_KEY_LEN..].try_into().unwrap();
                self.import_accounts_db_003(&key, &nonce, db)
            }

            2 => {
                let db = match obj.get("db").and_then(|v| v.as_str()) {
                    Some(s) => s,
                    None => return IMPORT_ERROR_INVALID_FILE,
                };

                let salt = match obj.get("salt").and_then(|v| v.as_str()) {
                    Some(s) => match data_encoding::BASE64.decode(s.as_bytes()) {
                        Ok(b) => b,
                        Err(_) => return IMPORT_ERROR_INVALID_FILE,
                    },
                    None => return IMPORT_ERROR_INVALID_FILE,
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
                    .hash_password_into(passphrase.as_bytes(), &salt, &mut out)
                    .is_err()
                {
                    return IMPORT_ERROR_UNKNOWN;
                }

                let key: [u8; XCHACHA20_KEY_SIZE] = match out[..XCHACHA20_KEY_SIZE].try_into() {
                    Ok(k) => k,
                    Err(_) => return IMPORT_ERROR_UNKNOWN,
                };

                let nonce: [u8; XCHACHA20_NONCE_SIZE] = match out[XCHACHA20_KEY_SIZE..].try_into() {
                    Ok(n) => n,
                    Err(_) => return IMPORT_ERROR_UNKNOWN,
                };

                self.import_accounts_db_002(db, &key, &nonce)
            }

            _ => IMPORT_ERROR_INCORRECT_VERSION,
        }
    }

    pub fn import_accounts_db_003(
        &mut self,
        key: &[u8; AEGIS256_KEY_LEN],
        nonce: &[u8; AEGIS256_NONCE_LEN],
        db: &str,
    ) -> i32 {
        use aegis::aegis256::Aegis256;

        let blob = match data_encoding::BASE64.decode(db.as_bytes()) {
            Ok(b) => b,
            Err(_) => return IMPORT_ERROR_INVALID_FILE,
        };

        if blob.len() < AEGIS256_TAG_LEN {
            return IMPORT_ERROR_INVALID_FILE;
        }

        let (ciphertext, tag_bytes) = blob.split_at(blob.len() - AEGIS256_TAG_LEN);
        let tag: [u8; AEGIS256_TAG_LEN] = match tag_bytes.try_into() {
            Ok(t) => t,
            Err(_) => return IMPORT_ERROR_INVALID_FILE,
        };

        let plaintext_bytes =
            match Aegis256::<AEGIS256_TAG_LEN>::new(key, nonce).decrypt(ciphertext, &tag, b"") {
                Ok(pt) => pt,
                Err(_) => return IMPORT_ERROR_INCORRECT_PASSPHRASE,
            };

        let json_array: serde_json::Value = match serde_json::from_slice(&plaintext_bytes) {
            Ok(val) => val,
            Err(_) => return IMPORT_ERROR_INVALID_FILE,
        };

        let json_array = match json_array.as_array() {
            Some(arr) => arr,
            None => return IMPORT_ERROR_INVALID_FILE,
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
                .and_then(|s| data_encoding::BASE64.decode(s.as_bytes()).ok())
                .and_then(|b| String::from_utf8(b).ok())
                .unwrap_or_default();

            let issuer = obj
                .get("issuer")
                .and_then(|v| v.as_str())
                .and_then(|s| data_encoding::BASE64.decode(s.as_bytes()).ok())
                .and_then(|b| String::from_utf8(b).ok())
                .unwrap_or_default();

            let note = obj
                .get("note")
                .and_then(|v| v.as_str())
                .and_then(|s| data_encoding::BASE64.decode(s.as_bytes()).ok())
                .and_then(|b| String::from_utf8(b).ok())
                .unwrap_or_default();

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
                .unwrap_or(utils::colors::WHITE);

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

        let imported_count = new_accounts.len() as i32;

        if imported_count == 0 {
            return IMPORT_ERROR_NO_ACCOUNTS;
        }

        for imported in new_accounts {
            if let Some(existing) = self.accounts.iter_mut().find(|a| a.id == imported.id) {
                *existing = imported;
            } else {
                self.accounts.push(imported);
            }
        }

        imported_count
    }

    pub fn import_accounts_db_002(
        &mut self,
        db: &str,
        key: &[u8; XCHACHA20_KEY_SIZE],
        nonce: &[u8; XCHACHA20_NONCE_SIZE],
    ) -> i32 {
        use chacha20poly1305::{KeyInit, aead::Aead};

        let ciphertext = match data_encoding::BASE64.decode(db.as_bytes()) {
            Ok(ct) => ct,
            Err(_) => return IMPORT_ERROR_INVALID_FILE,
        };

        let cipher = chacha20poly1305::XChaCha20Poly1305::new(key.into());
        let Ok(nonce) = chacha20poly1305::XNonce::try_from(nonce.as_slice()) else {
            return IMPORT_ERROR_INVALID_FILE;
        };

        let plaintext_bytes = match cipher.decrypt(&nonce, ciphertext.as_ref()) {
            Ok(pt) => pt,
            Err(_) => return IMPORT_ERROR_INCORRECT_PASSPHRASE,
        };

        let json_array: serde_json::Value = match serde_json::from_slice(&plaintext_bytes) {
            Ok(val) => val,
            Err(_) => return IMPORT_ERROR_INCORRECT_PASSPHRASE,
        };

        let json_array = match json_array.as_array() {
            Some(arr) => arr,
            None => return IMPORT_ERROR_INCORRECT_PASSPHRASE,
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
                .unwrap_or(utils::colors::WHITE);

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

        let imported_count = new_accounts.len() as i32;

        if imported_count == 0 {
            return IMPORT_ERROR_NO_ACCOUNTS;
        }

        for imported in new_accounts {
            if let Some(existing) = self.accounts.iter_mut().find(|a| a.id == imported.id) {
                *existing = imported;
            } else {
                self.accounts.push(imported);
            }
        }

        imported_count
    }
}
