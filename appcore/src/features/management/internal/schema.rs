use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct Format {
    pub version: u64,
    pub content: Vec<String>,
}
