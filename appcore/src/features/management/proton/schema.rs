use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct Format {
    pub version: i32,
    pub entries: Option<Vec<Entry>>,
    pub salt: Option<String>,
    pub content: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct Entry {
    pub id: String,
    pub content: Content,
    pub note: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct Content {
    pub uri: String,
    pub entry_type: String,
    pub name: String,
}
