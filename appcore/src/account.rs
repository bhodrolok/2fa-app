use zeroize::Zeroize;

use crate::features::otp::{algorithm::Algorithm, r#type::CodeType};

#[derive(Clone, Zeroize)]
pub struct Account {
    pub id: u64,
    pub name: String,
    pub issuer: String,
    pub note: String,
    pub secret: Vec<u8>,
    pub code_type: CodeType,
    pub algorithm: Algorithm,
    pub counter: u64,
    pub period: u64,
    pub digits: u32,
    pub color: u32,
}

#[derive(Clone, Zeroize)]
pub struct AccountShort {
    pub id: u64,
    pub name: String,
    pub issuer: String,
    pub note: String,
    pub code_type: CodeType,
    pub counter: u64,
    pub period: u64,
    pub color: u32,
}
