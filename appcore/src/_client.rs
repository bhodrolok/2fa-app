use zeroize::Zeroize;

use crate::account::Account;

pub const CLIENT_UNLOCK_PASSPHRASE_HASH_LEN: usize = 64;

pub struct Client {
    pub application_directory: String,
    pub unlock_passphrase_hash: [u8; CLIENT_UNLOCK_PASSPHRASE_HASH_LEN],
    pub edit: Option<Account>,
    pub accounts: Vec<Account>,
    pub hostages: Vec<Account>,
}

impl Client {
    pub fn unload(&mut self) {
        self.unlock_passphrase_hash.zeroize();

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
        self.unlock_passphrase_hash.copy_from_slice(hash);
    }

    pub fn change_unlock_passphrase_hash(&mut self, salt: &[u8], passphrase: &str) {
        let params = argon2::Params::new(256 * 1024, 4, 2, None).unwrap();
        let argon2 =
            argon2::Argon2::new(argon2::Algorithm::Argon2id, argon2::Version::V0x13, params);

        argon2
            .hash_password_into(
                passphrase.as_bytes(),
                salt,
                &mut self.unlock_passphrase_hash,
            )
            .unwrap();
    }

    pub fn get_unlock_passphrase_hash(&self) -> &[u8; CLIENT_UNLOCK_PASSPHRASE_HASH_LEN] {
        &self.unlock_passphrase_hash
    }
}

impl Default for Client {
    fn default() -> Self {
        Self {
            application_directory: String::new(),
            unlock_passphrase_hash: [0u8; CLIENT_UNLOCK_PASSPHRASE_HASH_LEN],
            edit: None,
            accounts: Vec::new(),
            hostages: Vec::new(),
        }
    }
}
