use serde::{Deserialize, Serialize};
use serde_json::Value;

#[derive(Debug, Serialize, Deserialize)]
pub struct Format {
    pub version: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub salt: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub hash: Option<String>,
    pub content: Value,
}

pub const VERSION: u64 = 4;
pub const SALT_LEN: usize = 16;
pub const ARGON2ID_M_COST: u32 = 256 * 1024; // 256 MiB
pub const ARGON2ID_T_COST: u32 = 4;
pub const ARGON2ID_P_COST: u32 = 2;
pub const KEY_LEN: usize = 64;
pub const HASH_LEN: usize = 16;
pub const DERIVED_LEN: usize = 64;
pub const OKM: &[u8] = b"app.ninesevennine.twofactorauthenticator.export.v4";
pub const AEGIS256_KEY_LEN: usize = 32;
pub const AEGIS256_NONCE_LEN: usize = 32;
pub const AEGIS256_AAD_LEN: usize = 64;
pub const AEGIS256_TAG_LEN: usize = 32;
