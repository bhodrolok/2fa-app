use crate::features::{old_otpauth::Account, otpauth::serialize::serialize};

pub fn export(accounts: &Vec<Account>) -> String {
    let mut result = String::new();

    for (i, account) in accounts.iter().enumerate() {
        if i > 0 {
            result.push('\n');
        }

        result.push_str(&serialize(account));
    }

    result
}
