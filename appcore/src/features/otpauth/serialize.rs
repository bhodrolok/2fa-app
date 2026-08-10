use percent_encoding::{NON_ALPHANUMERIC, utf8_percent_encode};

use crate::{
    features::old_otpauth::Account,
    utils::types::{Algorithm, CodeType},
};

pub fn v1(account: &Account) -> String {
    let code_type = match account.code_type {
        CodeType::HOTP => "hotp",
        CodeType::TOTP | CodeType::STEAM => "totp",
    };

    let issuer = match account.code_type {
        CodeType::STEAM => "Steam",
        _ => &account.issuer,
    };

    let name = utf8_percent_encode(&account.name, NON_ALPHANUMERIC).to_string();
    let issuer = utf8_percent_encode(&issuer, NON_ALPHANUMERIC).to_string();

    let label = match (issuer.is_empty(), name.is_empty()) {
        (true, true) => "unknown".to_string(),
        (true, false) => name.to_string(),
        (false, _) => format!("{issuer}:{name}"),
    };

    let secret = data_encoding::BASE32_NOPAD.encode(&account.secret);

    let mut query = vec![format!("secret={secret}")];

    if !issuer.is_empty() {
        query.push(format!("issuer={issuer}"));
    }

    if account.code_type != CodeType::STEAM {
        match account.algorithm {
            Algorithm::SHA1 => {}
            Algorithm::SHA256 => query.push("algorithm=SHA256".to_string()),
            Algorithm::SHA512 => query.push("algorithm=SHA512".to_string()),
        }

        if account.digits != 6 {
            query.push(format!("digits={}", account.digits));
        }

        match account.code_type {
            CodeType::HOTP => query.push(format!("counter={}", account.counter)),
            CodeType::TOTP | CodeType::STEAM if account.period != 30 => {
                query.push(format!("period={}", account.period));
            }
            _ => {}
        }
    }

    format!("otpauth://{code_type}/{label}?{}", query.join("&"))
}
