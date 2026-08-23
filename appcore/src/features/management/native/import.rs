use crate::{
    account::Account,
    features::{
        management::{native::schema::Format, result::ImportResult},
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

    let Some(content) = format.content.as_array() else {
        return ImportResult::InvalidFormat;
    };

    for uri in content {
        let Some(uri) = uri.as_str() else {
            return ImportResult::InvalidFormat;
        };

        if let Some(account) = deserialize(uri) {
            accounts.push(account);
        }
    }

    ImportResult::Success
}
