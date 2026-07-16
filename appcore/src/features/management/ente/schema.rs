use serde::Deserialize;

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Format {
    pub version: u32,
    pub kdf_params: KdfParams,
    pub encrypted_data: String,
    pub encryption_nonce: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct KdfParams {
    pub mem_limit: u32,
    pub ops_limit: u32,
    pub salt: String,
}
