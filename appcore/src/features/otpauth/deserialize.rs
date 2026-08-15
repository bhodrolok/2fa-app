use percent_encoding::percent_decode_str;

use crate::{
    features::old_otpauth::Account,
    utils::types::{Algorithm, CodeType},
};

// https://www.ietf.org/archive/id/draft-andesco-otpauth-uri-00.html
pub fn deserialize(uri: &str) -> Option<Account> {
    // 3. uri format
    let rest = uri.trim().strip_prefix("otpauth://")?;
    let (path, query) = rest.split_once('?')?;
    let (otp_type, label) = path.split_once('/')?;

    // 3.2. otp-type
    let mut code_type = match otp_type {
        "hotp" => CodeType::HOTP,
        "totp" => CodeType::TOTP,
        _ => return None,
    };

    // 3.3. label
    let label = percent_decode_str(label).decode_utf8().ok()?.into_owned();
    let (mut issuer, mut name) = match label.split_once(':') {
        Some((issuer, name)) => (issuer.to_string(), name.to_string()),
        None if label == "?" => (String::new(), String::new()),
        None => (String::new(), label),
    };

    // 3.4. parameters
    let mut id = 0;
    let mut _timestamp = 0; // Will be added later.
    let mut note = String::new();
    let mut secret = None;
    let mut algorithm = Algorithm::SHA1;
    let mut counter = 0;
    let mut period = 30;
    let mut digits = 6;
    let mut color = 0xFF000000;

    for parameter in query.split('&') {
        let (key, value) = parameter.split_once('=')?;

        match key {
            "id" => id = value.parse().ok()?,
            "timestamp" => _timestamp = value.parse().ok()?,
            "issuer" => issuer = percent_decode_str(value).decode_utf8().ok()?.into_owned(),
            "note" => note = percent_decode_str(value).decode_utf8().ok()?.into_owned(),
            "secret" => secret = data_encoding::BASE32_NOPAD.decode(value.as_bytes()).ok(),
            "algorithm" => {
                algorithm = match value {
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
                let bytes = data_encoding::HEXLOWER.decode(value.as_bytes()).ok()?;
                color = u32::from_be_bytes(bytes.try_into().ok()?);
            }
            _ => {}
        }
    }

    if name == issuer {
        name.clear();
    }

    if code_type == CodeType::TOTP && issuer.eq_ignore_ascii_case("Steam") {
        code_type = CodeType::STEAM;
        digits = 5;
        period = 30;
    }

    let secret = secret?;

    if secret.is_empty() || !(4..=10).contains(&digits) || period < 10 {
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
