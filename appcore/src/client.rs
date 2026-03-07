use crate::features::codegen::{Algorithm, CodeType, hotp, steam_custom};
use crate::features::otpauth::{self, Account, AccountTruncated};
use crate::utils;

pub const SEED_SIZE: usize = 64;
pub const AUTOMATIC_INTERNAL_BACKUPS_PASS_HASH_SIZE: usize = 56;
pub const AUTOMATIC_INTERNAL_BACKUPS_PASS_SALT_SIZE: usize = 16;
pub const SECRET_SIZE: usize = SEED_SIZE
    + AUTOMATIC_INTERNAL_BACKUPS_PASS_HASH_SIZE
    + AUTOMATIC_INTERNAL_BACKUPS_PASS_SALT_SIZE;

pub struct Client {
    pub seed: [u8; SEED_SIZE],
    pub automatic_internal_backups_pass_hash: [u8; AUTOMATIC_INTERNAL_BACKUPS_PASS_HASH_SIZE],
    pub automatic_internal_backups_pass_salt: [u8; AUTOMATIC_INTERNAL_BACKUPS_PASS_SALT_SIZE],
    pub edit: Option<Account>,
    pub accounts: Vec<Account>,
}

impl Client {
    pub fn set_automatic_internal_backups_pass(&mut self, pass: &str) {
        use sha3::Digest;

        let mut hasher = sha3::Sha3_512::new();
        hasher.update(&self.seed);

        let mut system_random = [0u8; 64];
        getrandom::fill(&mut system_random).unwrap();
        hasher.update(&system_random);

        let hash = hasher.finalize();

        let mut salt = [0u8; AUTOMATIC_INTERNAL_BACKUPS_PASS_SALT_SIZE];
        salt.copy_from_slice(&hash[..AUTOMATIC_INTERNAL_BACKUPS_PASS_SALT_SIZE]);

        // m: 256 MiB t: 4 p: 2
        let params = argon2::Params::new(256 * 1024, 4, 2, None).unwrap();
        let argon2 =
            argon2::Argon2::new(argon2::Algorithm::Argon2id, argon2::Version::V0x13, params);

        let mut out = [0u8; AUTOMATIC_INTERNAL_BACKUPS_PASS_HASH_SIZE];
        argon2
            .hash_password_into(pass.as_bytes(), &salt, &mut out)
            .unwrap();

        self.automatic_internal_backups_pass_hash = out;
        self.automatic_internal_backups_pass_salt = salt;
    }

    pub fn get_secret(&self) -> [u8; SECRET_SIZE] {
        let mut secret = [0u8; SECRET_SIZE];

        secret[..SEED_SIZE].copy_from_slice(&self.seed);

        secret[SEED_SIZE..SEED_SIZE + AUTOMATIC_INTERNAL_BACKUPS_PASS_HASH_SIZE]
            .copy_from_slice(&self.automatic_internal_backups_pass_hash);

        secret[SEED_SIZE + AUTOMATIC_INTERNAL_BACKUPS_PASS_HASH_SIZE..]
            .copy_from_slice(&self.automatic_internal_backups_pass_salt);

        secret
    }

    pub fn set_secret(&mut self, secret: &[u8; SECRET_SIZE]) {
        self.seed.copy_from_slice(&secret[..SEED_SIZE]);

        self.automatic_internal_backups_pass_hash.copy_from_slice(
            &secret[SEED_SIZE..SEED_SIZE + AUTOMATIC_INTERNAL_BACKUPS_PASS_HASH_SIZE],
        );

        self.automatic_internal_backups_pass_salt
            .copy_from_slice(&secret[SEED_SIZE + AUTOMATIC_INTERNAL_BACKUPS_PASS_HASH_SIZE..]);
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

    pub fn get_accounts_truncated_filtered(&self, query: &str) -> Vec<AccountTruncated> {
        if query.is_empty() {
            return self
                .accounts
                .iter()
                .map(|account| AccountTruncated {
                    id: account.id,
                    name: account.name.clone(),
                    issuer: account.issuer.clone(),
                    code_type: account.code_type,
                    counter: account.counter,
                    period: account.period,
                    digits: account.digits,
                    color: account.color,
                })
                .collect();
        }

        let query_lower = query.to_lowercase();

        fn ngram_overlap_score(query: &str, target: &str) -> f32 {
            let query_chars: Vec<char> = query.chars().collect();
            let target_chars: Vec<char> = target.chars().collect();

            let query_ngrams: Vec<(char, char)> =
                query_chars.windows(2).map(|w| (w[0], w[1])).collect();
            let target_ngrams: Vec<(char, char)> =
                target_chars.windows(2).map(|w| (w[0], w[1])).collect();

            if query_ngrams.is_empty() || target_ngrams.is_empty() {
                return 0.0;
            }

            let common_count = query_ngrams
                .iter()
                .filter(|bg| target_ngrams.contains(bg))
                .count();

            (2 * common_count) as f32 / (query_ngrams.len() + target_ngrams.len()) as f32
        }

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

                let mut name_score = ngram_overlap_score(&query_lower, &name_lower);
                let mut issuer_score = ngram_overlap_score(&query_lower, &issuer_lower);
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

        filtered.sort_unstable_by(|a, b| b.1.partial_cmp(&a.1).unwrap());

        filtered
            .into_iter()
            .map(|(account, _)| AccountTruncated {
                id: account.id,
                name: account.name.clone(),
                issuer: account.issuer.clone(),
                code_type: account.code_type,
                counter: account.counter,
                period: account.period,
                digits: account.digits,
                color: account.color,
            })
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
            CodeType::TOTP => hotp(
                &account.secret,
                account.algorithm,
                account.digits,
                final_counter,
            ),
            CodeType::HOTP => hotp(
                &account.secret,
                account.algorithm,
                account.digits,
                account.counter,
            ),
            CodeType::STEAM => steam_custom(&account.secret, final_counter * account.period),
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
        if let Some(pos) = self.accounts.iter().position(|a| a.id == id) {
            self.accounts.remove(pos);
        }
    }
}

impl Default for Client {
    fn default() -> Self {
        Self {
            seed: [0u8; SEED_SIZE],
            automatic_internal_backups_pass_hash: [0u8; AUTOMATIC_INTERNAL_BACKUPS_PASS_HASH_SIZE],
            automatic_internal_backups_pass_salt: [0u8; AUTOMATIC_INTERNAL_BACKUPS_PASS_SALT_SIZE],
            edit: None,
            accounts: Vec::new(),
        }
    }
}
