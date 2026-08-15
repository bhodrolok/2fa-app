use crate::features::{
    management::result::ImportResult, old_otpauth::Account, otpauth::deserialize::deserialize,
};

pub fn import(accounts: &mut Vec<Account>, txt: &str) -> ImportResult {
    for line in txt.lines() {
        let line = line.trim();

        if line.is_empty() {
            continue;
        }

        if let Some(mut account) = deserialize(line) {
            account.color = 0xFFFFFFFF;
            accounts.push(account);
        }
    }

    ImportResult::Success
}
