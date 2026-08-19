use crate::{
    account::Account,
    features::{
        management::{internal::schema::Format, result::ImportResult},
        otpauth::deserialize::deserialize,
    },
};

pub fn import(accounts: &mut Vec<Account>, json: &str) -> ImportResult {
    let format: Format = match serde_json::from_str(json) {
        Ok(f) => f,
        Err(_) => return ImportResult::InvalidFormat,
    };

    if format.version != 4 {
        return ImportResult::UnsupportedVersion;
    }

    for uri in &format.content {
        if let Some(account) = deserialize(uri) {
            accounts.push(account);
        }
    }

    ImportResult::Success
}
