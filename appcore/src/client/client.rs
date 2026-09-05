use crate::{
    account::Account,
    features::management::native::v4::schema::{KEY_LEN, SALT_LEN},
};

pub struct Client {
    pub loaded: bool,
    pub application_directory: String,
    pub salt: [u8; SALT_LEN],
    pub key: [u8; KEY_LEN],
    pub edit: Option<Account>,
    pub accounts: Vec<Account>,
    pub hostages: Vec<Account>,
}

impl Default for Client {
    fn default() -> Self {
        Self {
            loaded: false,
            application_directory: String::new(),
            salt: [0u8; 16],
            key: [0u8; 64],
            edit: None,
            accounts: Vec::new(),
            hostages: Vec::new(),
        }
    }
}
