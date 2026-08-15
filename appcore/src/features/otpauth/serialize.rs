use percent_encoding::{NON_ALPHANUMERIC, utf8_percent_encode};

use crate::{
    features::old_otpauth::Account,
    utils::types::{Algorithm, CodeType},
};

// https://www.ietf.org/archive/id/draft-andesco-otpauth-uri-00.html
pub fn serialize(account: &Account) -> String {
    // 3.2. otp-type
    let otp_type = match account.code_type {
        CodeType::HOTP => "hotp",
        CodeType::TOTP | CodeType::STEAM => "totp",
    };

    // 3.3. label
    let label = match (!account.name.is_empty(), !account.issuer.is_empty()) {
        (true, true) => format!(
            "{}%3A{}",
            utf8_percent_encode(&account.issuer.replace(':', "-"), NON_ALPHANUMERIC),
            utf8_percent_encode(&account.name.replace(':', "-"), NON_ALPHANUMERIC)
        ),
        (false, true) => {
            utf8_percent_encode(&account.issuer.replace(':', "-"), NON_ALPHANUMERIC).to_string()
        }
        (true, false) => {
            utf8_percent_encode(&account.name.replace(':', "-"), NON_ALPHANUMERIC).to_string()
        }
        _ => "%3F".to_string(),
    };

    // 3.4. parameters
    let mut parameters = vec![];

    // 3.4.1. secret
    parameters.push(format!(
        "secret={}",
        data_encoding::BASE32_NOPAD.encode(&account.secret)
    ));

    // 3.4.2. issuer
    if !account.issuer.is_empty() {
        parameters.push(format!(
            "issuer={}",
            utf8_percent_encode(&account.issuer, NON_ALPHANUMERIC)
        ));
    }

    // 3.4.3. algorithm
    match account.algorithm {
        Algorithm::SHA1 => {}
        Algorithm::SHA256 => parameters.push("algorithm=SHA256".to_string()),
        Algorithm::SHA512 => parameters.push("algorithm=SHA512".to_string()),
    }

    // 3.4.4. digits
    if account.digits != 6 {
        // We support digits from 4 to 10.
        parameters.push(format!("digits={}", account.digits));
    }

    // 3.4.5 period
    if account.code_type != CodeType::HOTP && account.period != 30 {
        parameters.push(format!("period={}", account.period));
    }

    // 3.4.6. counter
    if account.code_type == CodeType::HOTP {
        parameters.push(format!("counter={}", account.counter));
    }

    // 3.4.7 additonal parameters
    parameters.push(format!("id={}", account.id));
    parameters.push(format!("timestamp={}", 0)); // Will be added later.

    if !account.note.is_empty() {
        parameters.push(format!(
            "note={}",
            utf8_percent_encode(&account.note, NON_ALPHANUMERIC)
        ));
    }

    parameters.push(format!(
        "color={}",
        data_encoding::HEXLOWER.encode(&account.color.to_be_bytes())
    ));

    // 3. uri format
    format!("otpauth://{}/{}?{}", otp_type, label, parameters.join("&"))
}
