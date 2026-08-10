use crate::features::{old_otpauth::Account, otpauth};

pub fn v1(accounts: &Vec<Account>) -> String {
    let mut result = String::new();

    for (i, account) in accounts.iter().enumerate() {
        if i > 0 {
            result.push('\n');
        }

        result.push_str(&otpauth::serialize::v1(account));
    }

    result
}
