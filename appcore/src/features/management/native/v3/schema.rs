use serde::Deserialize;
use zeroize::Zeroize;

#[derive(Debug, Deserialize, Zeroize)]
pub struct Format {
    pub version: u64,
    pub salt: String,
    pub db: String,
}

#[derive(Debug, Deserialize, Zeroize)]
pub struct Entry {
    pub id: String,
    pub secret: String,
    pub code_type: String,
    #[zeroize(skip)]
    pub algorithm: Option<String>,
    #[zeroize(skip)]
    pub name: Option<String>,
    #[zeroize(skip)]
    pub issuer: Option<String>,
    #[zeroize(skip)]
    pub note: Option<String>,
    #[zeroize(skip)]
    pub period: Option<u64>,
    #[zeroize(skip)]
    pub digits: Option<u32>,
    #[zeroize(skip)]
    pub counter: Option<u64>,
    #[zeroize(skip)]
    pub color: Option<String>,
}

pub const VERSION: u64 = 3;
pub const ARGON2ID_M_COST: u32 = 256 * 1024; // 256 MiB
pub const ARGON2ID_T_COST: u32 = 4;
pub const ARGON2ID_P_COST: u32 = 2;
pub const KEY_LEN: usize = 64;
pub const AEGIS256_KEY_LEN: usize = 32;
pub const AEGIS256_NONCE_LEN: usize = 32;
pub const AEGIS256_AAD_LEN: usize = 64;
pub const AEGIS256_TAG_LEN: usize = 16;
