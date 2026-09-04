use zeroize::Zeroize;

use crate::{
    account::Account,
    features::management::native::v4::schema::{KEY_LEN, SALT_LEN},
};

pub struct Client {
    pub application_directory: String,
    pub salt: [u8; SALT_LEN],
    pub key: [u8; KEY_LEN],
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

    pub fn get_unlock_passphrase_hash(&self) -> &[u8; 64] {
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
