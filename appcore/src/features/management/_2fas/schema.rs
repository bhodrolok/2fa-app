use serde::Deserialize;

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Format {
    pub schema_version: u32,
    pub services: Option<Vec<Service>>,
    pub services_encrypted: Option<String>,
    pub reference: Option<String>,
    pub groups: Vec<Group>,
    pub updated_at: u64,
    pub app_version_code: u64,
    pub app_version_name: String,
    pub app_origin: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Service {
    pub name: String,
    pub secret: String,
    pub updated_at: u64,
    pub otp: Otp,
    pub order: Order,
    pub icon: Icon,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Otp {
    pub link: String,
    pub label: Option<String>,
    pub account: Option<String>,
    pub issuer: Option<String>,
    pub token_type: String,
    pub source: String,
}

#[derive(Debug, Deserialize)]
pub struct Order {
    pub position: u32,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Icon {
    pub selected: String,
    pub label: IconLabel,
    pub icon_collection: IconCollection,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct IconLabel {
    pub text: String,
    pub background_color: String,
}

#[derive(Debug, Deserialize)]
pub struct IconCollection {
    pub id: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Group {
    pub id: String,
    pub name: String,
    pub updated_at: u64,
}
