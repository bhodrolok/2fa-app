use std::collections::HashMap;

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

    // Icons
    pub slot: u32,
    pub name: String,
    pub timestamp: u64,
    // issuer, (textured/tinted, color)
    pub map: HashMap<String, (String, u32)>,
}

impl Default for Client {
    fn default() -> Self {
        Self {
            loaded: false,
            application_directory: String::new(),
            salt: [0u8; SALT_LEN],
            key: [0u8; KEY_LEN],
            edit: None,
            accounts: Vec::new(),
            hostages: Vec::new(),
            slot: 0,
            name: String::new(),
            timestamp: 0,
            map: HashMap::new(),
        }
    }
}
