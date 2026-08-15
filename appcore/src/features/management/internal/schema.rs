use serde::Serialize;

#[derive(Debug, Serialize)]
pub struct Format {
    pub version: u64,
    pub content: Content,
}

#[derive(Debug, Serialize)]
pub struct Content {
    pub accounts: Vec<String>,
}
