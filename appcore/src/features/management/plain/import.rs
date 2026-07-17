use crate::features::{
    management::result::ImportResult,
    otpauth::{self, Account},
};

pub fn v1(accounts: &mut Vec<Account>, txt: &str) -> ImportResult {
    for line in txt.lines() {
        let line = line.trim();

        if line.is_empty() {
            continue;
        }

        if let Some(account) = otpauth::parse(line) {
            accounts.push(account);
        }
    }

    ImportResult::Success
}
