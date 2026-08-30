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
