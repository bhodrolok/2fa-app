use crate::{
    account::Account,
    features::{
        management::internal::schema::{Content, Format},
        otpauth::serialize::serialize,
    },
};

pub fn export(accounts: &[Account]) -> String {
    let json = Format {
        version: 4,
        content: Content {
            accounts: accounts.iter().map(serialize).collect(),
        },
    };

    serde_json::to_string(&json).unwrap_or_default()
}
