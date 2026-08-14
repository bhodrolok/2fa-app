use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Format {
    pub services: Option<Vec<Service>>,
    pub groups: Vec<Group>,
    pub updated_at: u64,
    pub schema_version: u32,
    pub app_version_code: u64,
    pub app_version_name: String,
    pub app_origin: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub services_encrypted: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub reference: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Service {
    pub name: String,
    pub secret: String,
    pub updated_at: u64,
    pub otp: Otp,
    pub order: Order,
    pub icon: Icon,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Otp {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub link: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub label: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub account: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub issuer: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub digits: Option<u32>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub period: Option<u32>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub counter: Option<u64>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub algorithm: Option<String>,
    pub token_type: String,
    pub source: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Order {
    pub position: u32,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Icon {
    pub selected: String,
    pub label: IconLabel,
    pub icon_collection: IconCollection,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct IconLabel {
    pub text: String,
    pub background_color: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct IconCollection {
    pub id: String,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Group {
    pub id: String,
    pub name: String,
    pub updated_at: u64,
}
