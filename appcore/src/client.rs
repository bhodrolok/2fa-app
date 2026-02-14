use crate::features::codegen::{Algorithm, CodeType, hotp, steam_custom};
use crate::features::otpauth::{self, Account, AccountTruncated};
use crate::utils::color::generate_account_color;

pub const SEED_SIZE: usize = 64;
pub const AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_HASH_SIZE: usize = 56;
pub const AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_SALT_SIZE: usize = 16;
pub const SECRET_SIZE: usize = SEED_SIZE
    + AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_HASH_SIZE
    + AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_SALT_SIZE;

pub struct Client {
    pub seed: [u8; SEED_SIZE],
    pub automatic_internal_backups_password_hash:
        [u8; AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_HASH_SIZE],
    pub automatic_internal_backups_password_salt:
        [u8; AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_SALT_SIZE],
    pub edit: Option<Account>,
    pub accounts: Vec<Account>,
}

impl Client {
    pub fn set_automatic_internal_backups_password(&mut self, password: &str) {
        use sha3::Digest;

        let mut hasher = sha3::Sha3_512::new();
        hasher.update(&self.seed);

        let mut system_random = [0u8; 64];
        getrandom::fill(&mut system_random).unwrap();
        hasher.update(&system_random);

        let hash = hasher.finalize();

        let mut salt = [0u8; AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_SALT_SIZE];
        salt.copy_from_slice(&hash[..AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_SALT_SIZE]);

        // m: 256 MiB t: 4 p: 2
        let params = argon2::Params::new(256 * 1024, 4, 2, None).unwrap();
        let argon2 =
            argon2::Argon2::new(argon2::Algorithm::Argon2id, argon2::Version::V0x13, params);

        let mut out = [0u8; AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_HASH_SIZE];
        argon2
            .hash_password_into(password.as_bytes(), &salt, &mut out)
            .unwrap();

        self.automatic_internal_backups_password_hash = out;
        self.automatic_internal_backups_password_salt = salt;
    }

    pub fn get_secret(&self) -> [u8; SECRET_SIZE] {
        let mut secret = [0u8; SECRET_SIZE];

        secret[..SEED_SIZE].copy_from_slice(&self.seed);

        secret[SEED_SIZE..SEED_SIZE + AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_HASH_SIZE]
            .copy_from_slice(&self.automatic_internal_backups_password_hash);

        secret[SEED_SIZE + AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_HASH_SIZE..]
            .copy_from_slice(&self.automatic_internal_backups_password_salt);

        secret
    }

    pub fn set_secret(&mut self, secret: &[u8; SECRET_SIZE]) {
        self.seed.copy_from_slice(&secret[..SEED_SIZE]);

        self.automatic_internal_backups_password_hash
            .copy_from_slice(
                &secret[SEED_SIZE..SEED_SIZE + AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_HASH_SIZE],
            );

        self.automatic_internal_backups_password_salt
            .copy_from_slice(&secret[SEED_SIZE + AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_HASH_SIZE..]);
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
            color: generate_account_color(),
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
        let max_distance = match query_lower.len() {
            0..=2 => 1,
            3..=4 => 2,
            _ => 3,
        };

        let mut filtered: Vec<(&Account, usize)> = self
            .accounts
            .iter()
            .filter_map(|account| {
                let name_lower = account.name.to_lowercase();
                let issuer_lower = account.issuer.to_lowercase();
                let note_lower = account.note.to_lowercase();

                if name_lower.contains(&query_lower)
                    || issuer_lower.contains(&query_lower)
                    || note_lower.contains(&query_lower)
                {
                    let name_dist = strsim::levenshtein(&query_lower, &name_lower);
                    let issuer_dist = strsim::levenshtein(&query_lower, &issuer_lower);
                    return Some((account, name_dist.min(issuer_dist)));
                }

                let name_dist = strsim::levenshtein(&query_lower, &name_lower);
                let issuer_dist = strsim::levenshtein(&query_lower, &issuer_lower);
                let min_dist = name_dist.min(issuer_dist);

                if min_dist <= max_distance {
                    Some((account, min_dist))
                } else {
                    None
                }
            })
            .collect();

        filtered.sort_unstable_by_key(|(_, distance)| *distance);

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
            automatic_internal_backups_password_hash: [0u8;
                AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_HASH_SIZE],
            automatic_internal_backups_password_salt: [0u8;
                AUTOMATIC_INTERNAL_BACKUPS_PASSWORD_SALT_SIZE],
            edit: None,
            accounts: Vec::new(),
        }
    }
}
