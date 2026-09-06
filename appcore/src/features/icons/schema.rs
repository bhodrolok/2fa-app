use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct IconPack {
    pub name: String,
    pub timestamp: u64,
    pub icons: Vec<IconEntry>,
}

#[derive(Debug, Deserialize)]
pub struct IconEntry {
    pub issuer: Vec<String>,
    #[serde(default)]
    pub textured: Option<String>,
    #[serde(default)]
    pub tinted: Option<String>,
    pub color: String,
}
