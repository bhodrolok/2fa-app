use percent_encoding::percent_decode_str;
use zeroize::{Zeroize, Zeroizing};

use crate::{
    account::Account,
    features::otp::{algorithm::Algorithm, r#type::CodeType},
};

// https://www.ietf.org/archive/id/draft-andesco-otpauth-uri-00.html
pub fn deserialize(uri: &str) -> Option<Account> {
    // 3. uri format
    let (scheme, rest) = uri.trim().split_once("://")?;
    if !scheme.eq_ignore_ascii_case("otpauth") {
        return None;
    }

    let (path, query) = rest.split_once('?').unwrap_or((rest, ""));
    let (otp_type, label) = path.split_once('/').unwrap_or((path, ""));

    // 3.2. otp-type
    let mut code_type = match otp_type.to_ascii_lowercase().as_str() {
        "hotp" => CodeType::HOTP,
        "totp" => CodeType::TOTP,
        _ => return None,
    };

    // 3.3. label
    let label = percent_decode_str(label).decode_utf8().ok()?.into_owned();
    let (mut issuer, mut name) = match label.split_once(':') {
        Some((issuer, name)) => (issuer.trim().to_string(), name.trim().to_string()),
        None if label == "?" => (String::new(), String::new()),
        None => (String::new(), label.trim().to_string()),
    };

    // 3.4. parameters
    let mut id = getrandom::u64().unwrap();
    let mut note = String::new();
    let mut secret = Vec::new();
    let mut algorithm = Algorithm::SHA1;
    let mut counter = 0;
    let mut period = 30;
    let mut digits = 6;
    let mut color = 0xFF808080;

    for parameter in query.split('&') {
        let (key, value) = parameter.split_once('=').unwrap_or((parameter, ""));

        match key.to_ascii_lowercase().as_str() {
            "id" => {
                let bytes = data_encoding::HEXLOWER_PERMISSIVE
                    .decode(value.as_bytes())
                    .ok()?;
                id = u64::from_be_bytes(bytes.try_into().ok()?);
            }
            "name" => {
                name = percent_decode_str(value)
                    .decode_utf8()
                    .ok()?
                    .trim()
                    .to_string()
            }
            "issuer" => {
                issuer = percent_decode_str(value)
                    .decode_utf8()
                    .ok()?
                    .trim()
                    .to_string()
            }
            "note" => note = percent_decode_str(value).decode_utf8().ok()?.into_owned(),
            "secret" => {
                let cleaned = Zeroizing::new(value.replace(['=', ' ', '-'], ""));
                let upper = Zeroizing::new(cleaned.to_ascii_uppercase());
                secret = data_encoding::BASE32_NOPAD.decode(upper.as_bytes()).ok()?;
            }
            "algorithm" => {
                algorithm = match value.to_ascii_uppercase().replace('-', "").as_str() {
                    "SHA1" => Algorithm::SHA1,
                    "SHA256" => Algorithm::SHA256,
                    "SHA512" => Algorithm::SHA512,
                    _ => return None,
                }
            }
            "counter" => counter = value.parse().ok()?,
            "period" => period = value.parse().ok()?,
            "digits" => digits = value.parse().ok()?,
            "color" => {
                if let Ok(bytes) = data_encoding::HEXLOWER_PERMISSIVE.decode(value.as_bytes())
                    && let Ok(bytes) = bytes.try_into()
                {
                    color = u32::from_be_bytes(bytes);
                }
            }
            _ => {}
        }
    }

    if name == issuer {
        name.clear();
    }

    if code_type == CodeType::TOTP && issuer.eq_ignore_ascii_case("Steam") {
        code_type = CodeType::STEAM;
        algorithm = Algorithm::SHA1;
        period = 30;
        digits = 5;
    }

    if secret.is_empty() || !(4..=10).contains(&digits) || period < 10 {
        secret.zeroize();

        return None;
    }

    Some(Account {
        id,
        name,
        issuer,
        secret,
        code_type,
        algorithm,
        digits,
        period,
        counter,
        note,
        color,
    })
}
