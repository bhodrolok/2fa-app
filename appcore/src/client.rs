use zeroize::Zeroize;

use crate::features::otpauth::{self, Account, AccountTruncated};
use crate::features::{hotp, steam};
use crate::utils;
use crate::utils::types::{Algorithm, CodeType};

pub const CLIENT_UNLOCK_PASSPHRASE_HASH_LEN: usize = 64;

pub struct Client {
    pub unlock_passphrase_hash: [u8; CLIENT_UNLOCK_PASSPHRASE_HASH_LEN],
    pub edit: Option<Account>,
    pub accounts: Vec<Account>,
    pub hostages: Vec<Account>,
}

impl Client {
    pub fn unload(&mut self) {
        self.unlock_passphrase_hash.zeroize();

        if let Some(edit) = &mut self.edit {
            edit.zeroize();
        }
        self.edit = None;

        for account in &mut self.accounts {
            account.zeroize();
        }
        self.accounts.clear();
    }

    pub fn set_unlock_passphrase_hash(&mut self, hash: &[u8; CLIENT_UNLOCK_PASSPHRASE_HASH_LEN]) {
        self.unlock_passphrase_hash.copy_from_slice(hash);
    }

    pub fn change_unlock_passphrase_hash(&mut self, salt: &[u8], passphrase: &str) {
        let params = argon2::Params::new(256 * 1024, 4, 2, None).unwrap();
        let argon2 =
            argon2::Argon2::new(argon2::Algorithm::Argon2id, argon2::Version::V0x13, params);

        argon2
            .hash_password_into(
                passphrase.as_bytes(),
                salt,
                &mut self.unlock_passphrase_hash,
            )
            .unwrap();
    }

    pub fn get_unlock_passphrase_hash(&self) -> &[u8; CLIENT_UNLOCK_PASSPHRASE_HASH_LEN] {
        &self.unlock_passphrase_hash
    }

    pub fn set_edit_account_empty(&mut self) {
        self.edit = Some(Account {
            id: getrandom::u64().unwrap_or(0),
            name: String::new(),
            issuer: String::new(),
            note: String::new(),
            secret: Vec::new(),
            code_type: CodeType::TOTP,
            algorithm: Algorithm::SHA1,
            digits: 6,
            counter: 0,
            period: 30,
            color: utils::colors::BLACK,
        });
    }

    pub fn set_edit_account_from_otpauth(&mut self, url: &str) -> bool {
        if let Some(account) = otpauth::parse(url) {
            self.edit = Some(account);
            true
        } else {
            false
        }
    }

    pub fn copy_edit_account_into_accounts(&mut self) {
        if let Some(edit) = &self.edit {
            if let Some(existing) = self.accounts.iter_mut().find(|a| a.id == edit.id) {
                *existing = edit.clone();
            } else {
                self.accounts.push(edit.clone());
            }
        }
    }

    pub fn copy_account_into_edit_account(&mut self, id: u64) {
        if let Some(account) = self.accounts.iter().find(|a| a.id == id) {
            self.edit = Some(account.clone());
        }
    }

    fn truncate_account(account: &Account) -> AccountTruncated {
        AccountTruncated {
            id: account.id,
            name: account.name.clone(),
            issuer: account.issuer.clone(),
            code_type: account.code_type,
            counter: account.counter,
            period: account.period,
            digits: account.digits,
            color: account.color,
        }
    }

    pub fn get_accounts_truncated_filtered(&self, query: &str) -> Vec<AccountTruncated> {
        if query.is_empty() {
            return self.accounts.iter().map(Self::truncate_account).collect();
        }

        let query_lower = query.to_lowercase();

        let query_chars: Vec<char> = query_lower.chars().collect();
        let query_ngrams: Vec<(char, char)> =
            query_chars.windows(2).map(|w| (w[0], w[1])).collect();

        let ngram_overlap_score = |target: &str| -> f32 {
            let target_ngrams: Vec<(char, char)> = target
                .chars()
                .collect::<Vec<char>>()
                .windows(2)
                .map(|w| (w[0], w[1]))
                .collect();

            if query_ngrams.is_empty() || target_ngrams.is_empty() {
                return 0.0;
            }

            let common_count = query_ngrams
                .iter()
                .filter(|bg| target_ngrams.contains(bg))
                .count();

            (2 * common_count) as f32 / (query_ngrams.len() + target_ngrams.len()) as f32
        };

        let threshold: f32 = match query_lower.len() {
            0..=3 => 0.3,
            4..=6 => 0.5,
            _ => 0.6,
        };

        let mut filtered: Vec<(&Account, f32)> = self
            .accounts
            .iter()
            .filter_map(|account| {
                let name_lower = account.name.to_lowercase();
                let issuer_lower = account.issuer.to_lowercase();
                let note_lower = account.note.to_lowercase();

                let mut name_score = ngram_overlap_score(&name_lower);
                let mut issuer_score = ngram_overlap_score(&issuer_lower);
                let note_score = if note_lower.contains(&query_lower) {
                    1.0
                } else {
                    0.0
                };

                if name_lower.contains(&query_lower) {
                    name_score = name_score.max(1.0);
                }

                if issuer_lower.contains(&query_lower) {
                    issuer_score = issuer_score.max(1.0);
                }

                let best_score = name_score.max(issuer_score).max(note_score);

                if best_score >= threshold {
                    Some((account, best_score))
                } else {
                    None
                }
            })
            .collect();

        filtered
            .sort_unstable_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

        filtered
            .into_iter()
            .map(|(account, _)| Self::truncate_account(account))
            .collect()
    }

    pub fn get_account_code(&self, id: u64, counter: u64, show_next: bool) -> String {
        let account = match &self.edit {
            Some(edit_account) if edit_account.id == id => edit_account,
            _ => match self.accounts.iter().find(|a| a.id == id) {
                Some(account) => account,
                None => return String::new(),
            },
        };

        if account.secret.is_empty() || !(4..=10).contains(&account.digits) {
            return String::new();
        }

        if matches!(account.code_type, CodeType::TOTP | CodeType::STEAM) && account.period < 10 {
            return String::new();
        }

        let final_counter = if show_next { counter + 1 } else { counter };

        match account.code_type {
            CodeType::TOTP => hotp::generate(
                &account.secret,
                account.algorithm,
                account.digits,
                final_counter,
            ),
            CodeType::HOTP => hotp::generate(
                &account.secret,
                account.algorithm,
                account.digits,
                account.counter,
            ),
            CodeType::STEAM => steam::generate(&account.secret, final_counter * account.period),
        }
    }

    pub fn reorder_account(&mut self, from_index: usize, to_index: usize) {
        if from_index >= self.accounts.len() || to_index >= self.accounts.len() {
            return;
        }

        if from_index == to_index {
            return;
        }

        if from_index < to_index {
            self.accounts[from_index..=to_index].rotate_left(1);
        } else {
            self.accounts[to_index..=from_index].rotate_right(1);
        }
    }

    pub fn delete_account(&mut self, id: u64) {
        self.accounts.retain(|a| a.id != id);
    }
}

impl Default for Client {
    fn default() -> Self {
        Self {
            unlock_passphrase_hash: [0u8; CLIENT_UNLOCK_PASSPHRASE_HASH_LEN],
            edit: None,
            accounts: Vec::new(),
            hostages: Vec::new(),
        }
    }
}
