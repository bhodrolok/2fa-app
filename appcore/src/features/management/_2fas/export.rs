use std::time::{SystemTime, UNIX_EPOCH};

use aes_gcm::{
    Aes256Gcm, Key,
    aead::{Aead, Nonce},
};
use hmac::KeyInit;
use pbkdf2::pbkdf2_hmac;
use sha2::Sha256;

use crate::{
    account::Account,
    features::{
        management::_2fas::schema::{Format, Icon, IconCollection, IconLabel, Order, Otp, Service},
        otp::{algorithm::Algorithm, r#type::CodeType},
        otpauth::serialize::serialize,
    },
};

pub fn export(accounts: &[Account], passphrase: &str) -> String {
    if passphrase.is_empty() {
        return String::new();
    }

    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as u64)
        .unwrap_or(0);

    let mut services = Vec::with_capacity(accounts.len());

    for (index, account) in accounts.iter().enumerate() {
        let (token_type, digits, period, counter) = match account.code_type {
            CodeType::TOTP => ("TOTP", account.digits, account.period as u32, None),
            CodeType::HOTP => ("HOTP", account.digits, 30, Some(account.counter)),
            CodeType::STEAM => ("STEAM", 5, 30, None),
        };

        let algorithm = match (account.code_type, account.algorithm) {
            (CodeType::STEAM, _) => "SHA1",
            (_, Algorithm::SHA1) => "SHA1",
            (_, Algorithm::SHA256) => "SHA256",
            (_, Algorithm::SHA512) => "SHA512",
        };

        let issuer = match account.code_type {
            CodeType::STEAM => "Steam".to_string(),
            _ => account.issuer.clone(),
        };

        let name = if !issuer.is_empty() {
            issuer.clone()
        } else if !account.name.is_empty() {
            account.name.clone()
        } else {
            "Unknown".to_string()
        };

        services.push(Service {
            name: name.clone(),
            secret: data_encoding::BASE32_NOPAD.encode(&account.secret),
            updated_at: now,
            otp: Otp {
                link: Some(serialize(account)),
                label: if account.name.is_empty() {
                    None
                } else {
                    Some(account.name.clone())
                },
                account: if account.name.is_empty() {
                    None
                } else {
                    Some(account.name.clone())
                },
                issuer: if issuer.is_empty() {
                    None
                } else {
                    Some(issuer)
                },
                digits: if digits == 6 { None } else { Some(digits) },
                period: if period == 30 { None } else { Some(period) },
                counter,
                algorithm: if algorithm == "SHA1" {
                    None
                } else {
                    Some(algorithm.to_string())
                },
                token_type: token_type.to_string(),
                source: "Link".to_string(),
            },
            order: Order {
                position: index as u32,
            },
            icon: Icon {
                selected: "Label".to_string(),
                label: IconLabel {
                    text: name.chars().take(2).collect::<String>().to_uppercase(),
                    background_color: "Indigo".to_string(),
                },
                icon_collection: IconCollection {
                    id: "a5b3fb65-4ec5-43e6-8ec1-49e24ca9e7ad".to_string(),
                },
            },
        });
    }

    let plaintext = match serde_json::to_vec(&services) {
        Ok(b) => b,
        Err(_) => return String::new(),
    };

    let mut salt = [0u8; 32];
    let mut iv = [0u8; 12];

    if getrandom::fill(&mut salt).is_err() || getrandom::fill(&mut iv).is_err() {
        return String::new();
    }

    let mut derived_key = [0u8; 32];
    pbkdf2_hmac::<Sha256>(passphrase.as_bytes(), &salt, 10_000, &mut derived_key);

    let key: &Key<Aes256Gcm> = match derived_key.as_slice().try_into() {
        Ok(k) => k,
        Err(_) => return String::new(),
    };

    let nonce: &Nonce<Aes256Gcm> = match iv.as_slice().try_into() {
        Ok(n) => n,
        Err(_) => return String::new(),
    };

    let cipher = Aes256Gcm::new(key);
    let ciphertext = match cipher.encrypt(nonce, plaintext.as_slice()) {
        Ok(c) => c,
        Err(_) => return String::new(),
    };

    let services_encrypted = format!(
        "{}:{}:{}",
        data_encoding::BASE64.encode(&ciphertext),
        data_encoding::BASE64.encode(&salt),
        data_encoding::BASE64.encode(&iv)
    );

    let mut ref_iv = [0u8; 12];
    if getrandom::fill(&mut ref_iv).is_err() {
        return String::new();
    }

    let ref_nonce: &Nonce<Aes256Gcm> = match ref_iv.as_slice().try_into() {
        Ok(n) => n,
        Err(_) => return String::new(),
    };

    let ref_plaintext = b"2FAS";
    let ref_ciphertext = match cipher.encrypt(ref_nonce, ref_plaintext.as_ref()) {
        Ok(c) => c,
        Err(_) => return String::new(),
    };

    let reference_encrypted = format!(
        "{}:{}:{}",
        data_encoding::BASE64.encode(&ref_ciphertext),
        data_encoding::BASE64.encode(&salt),
        data_encoding::BASE64.encode(&ref_iv)
    );

    let format = Format {
        services: Some(Vec::new()),
        groups: Vec::new(),
        updated_at: now,
        schema_version: 4,
        app_version_code: 5000034,
        app_version_name: "5.5.3".to_string(),
        app_origin: "android".to_string(),
        services_encrypted: Some(services_encrypted),
        reference: Some(reference_encrypted),
    };

    serde_json::to_string(&format).unwrap_or_default()
}
