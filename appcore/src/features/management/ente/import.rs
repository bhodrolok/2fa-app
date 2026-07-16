use crate::features::{
    management::result::ImportResult,
    otpauth::{self, Account},
};

// Ente encrypted exports use a non-standard XChaCha20-Poly1305 implementation and won't be supported.
pub fn plaintxt(accounts: &mut Vec<Account>, txt: &str) -> ImportResult {
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
