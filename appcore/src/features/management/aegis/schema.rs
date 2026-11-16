use serde::{Deserialize, Serialize};
use serde_json::Value;
use zeroize::Zeroize;

#[derive(Debug, Serialize, Deserialize)]
pub struct Vault {
    pub version: u32,
    pub header: Header,
    pub db: Value,
}

#[derive(Debug, Serialize, Deserialize, Zeroize)]
pub struct Header {
    pub slots: Option<Vec<Slot>>,
    pub params: Option<Params>,
}

#[derive(Debug, Serialize, Deserialize, Zeroize)]
pub struct Slot {
    #[serde(rename = "type")]
    pub slot_type: u32,
    pub uuid: String,
    pub key: String,
    pub key_params: Params,
    pub n: u32,
    pub r: u32,
    pub p: u32,
    pub salt: String,
    pub repaired: bool,
    pub is_backup: bool,
}

#[derive(Debug, Serialize, Deserialize, Zeroize)]
pub struct Params {
    pub nonce: String,
    pub tag: String,
}

#[derive(Debug, Serialize, Deserialize, Zeroize)]
pub struct Database {
    pub version: u32,
    pub entries: Vec<Entry>,
    pub groups: Vec<Group>,
}

#[derive(Debug, Serialize, Deserialize, Zeroize)]
pub struct Group {
    pub uuid: String,
    pub name: String,
}

#[derive(Debug, Serialize, Deserialize, Zeroize)]
pub struct Entry {
    #[serde(rename = "type")]
    pub entry_type: String,
    pub uuid: String,
    pub name: String,
    pub issuer: String,
    pub note: String,
    pub favorite: bool,
    pub icon: Option<String>,
    pub icon_mime: Option<String>,
    pub icon_hash: Option<String>,
    pub groups: Vec<String>,
    pub info: EntryInfo,
}

#[derive(Debug, Serialize, Deserialize, Zeroize)]
pub struct EntryInfo {
    pub secret: String,
    pub algo: String,
    pub digits: u32,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub period: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub counter: Option<u64>,
}

pub const AEGIS_SCRYPT_MAX_N_TIMES_R: u64 = (1 << 20) * 8; // max 1 GiB
pub const AEGIS_SCRYPT_MAX_P: u32 = 4;
