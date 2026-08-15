use crate::{account::Account, features::otpauth::serialize::serialize};

pub fn export(accounts: &[Account]) -> String {
    let mut result = String::new();

    for (i, account) in accounts.iter().enumerate() {
        if i > 0 {
            result.push('\n');
        }

        result.push_str(&serialize(account));
    }

    result
}
