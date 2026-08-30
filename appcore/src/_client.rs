use zeroize::Zeroize;

use crate::account::Account;

pub const CLIENT_UNLOCK_PASSPHRASE_HASH_LEN: usize = 64;

pub struct Client {
    pub application_directory: String,
    pub salt: [u8; 16],
    pub key: [u8; 64],
    pub edit: Option<Account>,
    pub accounts: Vec<Account>,
    pub hostages: Vec<Account>,
}

impl Client {
    pub fn unload(&mut self) {
        self.key.zeroize();

        if let Some(edit) = &mut self.edit {
            edit.zeroize();
        }
        self.edit = None;

        for account in &mut self.accounts {
            account.zeroize();
        }
        self.accounts.clear();
    }

    pub fn set_unlock_passphrase_hash(&mut self, hash: &[u8; CLIENT_UNLOCK_PASSPHRASE_HASH_LEN]) {
        self.key.copy_from_slice(hash);
    }

    pub fn change_unlock_passphrase_hash(&mut self, salt: &[u8], passphrase: &str) {
        let params = argon2::Params::new(256 * 1024, 4, 2, None).unwrap();
        let argon2 =
            argon2::Argon2::new(argon2::Algorithm::Argon2id, argon2::Version::V0x13, params);

        argon2
            .hash_password_into(passphrase.as_bytes(), salt, &mut self.key)
            .unwrap();
    }

    pub fn get_unlock_passphrase_hash(&self) -> &[u8; CLIENT_UNLOCK_PASSPHRASE_HASH_LEN] {
        &self.key
    }
}

impl Default for Client {
    fn default() -> Self {
        Self {
            application_directory: String::new(),
            salt: [0u8; 16],
            key: [0u8; 64],
            edit: None,
            accounts: Vec::new(),
            hostages: Vec::new(),
        }
    }
}
