use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct Format {
    pub version: u64,
    pub salt: String,
    pub db: String,
}

#[derive(Debug, Deserialize)]
pub struct Entry {
    pub id: String,
    pub secret: String,
    pub code_type: String,
    pub algorithm: Option<String>,
    pub name: Option<String>,
    pub issuer: Option<String>,
    pub note: Option<String>,
    pub period: Option<u64>,
    pub digits: Option<u32>,
    pub counter: Option<u64>,
    pub color: Option<String>,
}
