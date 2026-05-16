use crate::{
    features::codegen::{Algorithm, CodeType},
    utils::{self},
};
use url::Url;
use zeroize::Zeroize;

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

#[derive(Clone)]
pub struct AccountTruncated {
    pub id: u64,
    pub name: String,
    pub issuer: String,
    pub code_type: CodeType,
    pub counter: u64,
    pub period: u64,
    pub digits: u32,
    pub color: u32,
}

pub fn parse(url: &str) -> Option<Account> {
    let url = Url::parse(url).ok()?;

    if url.scheme() != "otpauth" {
        return None;
    }

    let host = url.host_str()?;
    let mut code_type = if host.eq_ignore_ascii_case("hotp") {
        CodeType::HOTP
    } else if host.eq_ignore_ascii_case("totp") {
        CodeType::TOTP
    } else {
        return None;
    };

    let raw_label = url.path().strip_prefix('/')?;
    let label = percent_encoding::percent_decode_str(raw_label).decode_utf8().ok()?;

    let (label_issuer, name) = match label.split_once(':') {
        Some((i, n)) => (i.to_string(), n.trim_start().to_string()),
        None => (String::new(), label.into_owned()),
    };

    let mut secret_str = None;
    let mut algorithm = Algorithm::SHA1;
    let mut digits = 6;
    let mut counter = 0;
    let mut period = 30;
    let mut issuer = label_issuer;

    for (key, value) in url.query_pairs() {
        match key.as_ref() {
            "secret" => secret_str = Some(value.into_owned()),
            "issuer" => issuer = value.into_owned(),
            "algorithm" => {
                algorithm = if value.eq_ignore_ascii_case("SHA256") {
                    Algorithm::SHA256
                } else if value.eq_ignore_ascii_case("SHA512") {
                    Algorithm::SHA512
                } else {
                    Algorithm::SHA1
                };
            }
            "digits" => digits = value.parse().unwrap_or(digits),
            "counter" => counter = value.parse().unwrap_or(counter),
            "period" => period = value.parse().unwrap_or(period),
            _ => {}
        }
    }

    if issuer.eq_ignore_ascii_case("steam") {
        code_type = CodeType::STEAM;
        digits = 5;
        period = 30;
    }

    let secret_str = secret_str?;
    let secret = data_encoding::BASE32_NOPAD
        .decode(secret_str.to_uppercase().as_bytes())
        .ok()?;

    Some(Account {
        id: getrandom::u64().unwrap_or(0),
        name,
        issuer,
        note: String::new(),
        secret,
        code_type,
        algorithm,
        digits,
        counter,
        period,
        color: utils::colors::BLACK,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_hotp() {
        let test_vectors = vec![
            // (url, expected_code_type, expected_issuer, expected_name, expected_algorithm, expected_digits, expected_period, expected_counter, expected_secret)
            (
                "otpauth://hotp/Provider:myaccount?secret=JBSWY3DPEHPK3PXP&counter=42",
                CodeType::HOTP,
                "Provider",
                "myaccount",
                Algorithm::SHA1,
                6,
                30,
                42,
                vec![0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21, 0xde, 0xad, 0xbe, 0xef],
            ),
            (
                "otpauth://hotp/Service:user?secret=JBSWY3DPEHPK3PXP&counter=999&algorithm=SHA256&digits=8",
                CodeType::HOTP,
                "Service",
                "user",
                Algorithm::SHA256,
                8,
                30,
                999,
                vec![0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21, 0xde, 0xad, 0xbe, 0xef],
            ),
            (
                "otpauth://hotp/Google:user?secret=JBSWY3DPEHPK3PXP&counter=50&issuer=CustomIssuer",
                CodeType::HOTP,
                "CustomIssuer",
                "user",
                Algorithm::SHA1,
                6,
                30,
                50,
                vec![0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21, 0xde, 0xad, 0xbe, 0xef],
            ),
            (
                "otpauth://hotp/MyIssuer?secret=JBSWY3DPEHPK3PXP&counter=1",
                CodeType::HOTP,
                "",
                "MyIssuer",
                Algorithm::SHA1,
                6,
                30,
                1,
                vec![0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21, 0xde, 0xad, 0xbe, 0xef],
            ),
        ];

        for (
            url,
            expected_code_type,
            expected_issuer,
            expected_name,
            expected_algorithm,
            expected_digits,
            expected_period,
            expected_counter,
            expected_secret,
        ) in test_vectors
        {
            let account = parse(url).expect("failed to parse hotp url");
            assert_eq!(account.code_type, expected_code_type);
            assert_eq!(account.issuer, expected_issuer);
            assert_eq!(account.name, expected_name);
            assert_eq!(account.algorithm, expected_algorithm);
            assert_eq!(account.digits, expected_digits);
            assert_eq!(account.period, expected_period);
            assert_eq!(account.counter, expected_counter);
            assert_eq!(account.secret, expected_secret);
        }
    }

    #[test]
    fn test_totp() {
        let test_vectors = vec![
            // (url, expected_code_type, expected_issuer, expected_name, expected_algorithm, expected_digits, expected_period, expected_counter, expected_secret)
            (
                "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
                CodeType::TOTP,
                "Example",
                "alice@google.com",
                Algorithm::SHA1,
                6,
                30,
                0,
                vec![0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21, 0xde, 0xad, 0xbe, 0xef],
            ),
            (
                "otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&algorithm=SHA512&period=60&digits=7",
                CodeType::TOTP,
                "GitHub",
                "user@example.com",
                Algorithm::SHA512,
                7,
                60,
                0,
                vec![0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21, 0xde, 0xad, 0xbe, 0xef],
            ),
            (
                "otpauth://totp/ACME:john@email.com?secret=JBSWY3DPEHPK3PXP&algorithm=SHA256&issuer=ACME",
                CodeType::TOTP,
                "ACME",
                "john@email.com",
                Algorithm::SHA256,
                6,
                30,
                0,
                vec![0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21, 0xde, 0xad, 0xbe, 0xef],
            ),
            (
                "otpauth://totp/Test:user?secret=JBSWY3DPEHPK3PXP&algorithm=sha256",
                CodeType::TOTP,
                "Test",
                "user",
                Algorithm::SHA256,
                6,
                30,
                0,
                vec![0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21, 0xde, 0xad, 0xbe, 0xef],
            ),
            (
                "otpauth://totp/Test:user?secret=jbsWy3dpehpk3pxp",
                CodeType::TOTP,
                "Test",
                "user",
                Algorithm::SHA1,
                6,
                30,
                0,
                vec![0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21, 0xde, 0xad, 0xbe, 0xef],
            ),
        ];

        for (
            url,
            expected_code_type,
            expected_issuer,
            expected_name,
            expected_algorithm,
            expected_digits,
            expected_period,
            expected_counter,
            expected_secret,
        ) in test_vectors
        {
            let account = parse(url).expect("failed to parse totp url");
            assert_eq!(account.code_type, expected_code_type);
            assert_eq!(account.issuer, expected_issuer);
            assert_eq!(account.name, expected_name);
            assert_eq!(account.algorithm, expected_algorithm);
            assert_eq!(account.digits, expected_digits);
            assert_eq!(account.period, expected_period);
            assert_eq!(account.counter, expected_counter);
            assert_eq!(account.secret, expected_secret);
        }
    }

    #[test]
    fn test_steam() {
        let test_vectors = vec![
            // (url, expected_name, expected_secret)
            (
                "otpauth://totp/Steam:juanpi1588?secret=MQ3XOP2I75XORCQIEKT63DMOIU2WDT7F&issuer=Steam",
                "juanpi1588",
                vec![
                    0x64, 0x37, 0x77, 0x3f, 0x48, 0xff, 0x6e, 0xe8, 0x8a, 0x08, 0x22, 0xa7, 0xed,
                    0x8d, 0x8e, 0x45, 0x35, 0x61, 0xcf, 0xe5,
                ],
            ),
            (
                "otpauth://totp/Steam:xkce27?secret=ETPBCTYOI34ACGK7CAXBBJUUCAKUH2I2&issuer=Steam",
                "xkce27",
                vec![
                    0x24, 0xde, 0x11, 0x4f, 0x0e, 0x46, 0xf8, 0x01, 0x19, 0x5f, 0x10, 0x2e, 0x10,
                    0xa6, 0x94, 0x10, 0x15, 0x43, 0xe9, 0x1a,
                ],
            ),
            (
                "otpauth://totp/Steam:trikce?secret=R7RDTIMSRCVE7CS7DWEVKHOVRBLFYXWC&issuer=Steam",
                "trikce",
                vec![
                    0x8f, 0xe2, 0x39, 0xa1, 0x92, 0x88, 0xaa, 0x4f, 0x8a, 0x5f, 0x1d, 0x89, 0x55,
                    0x1d, 0xd5, 0x88, 0x56, 0x5c, 0x5e, 0xc2,
                ],
            ),
        ];

        for (url, expected_name, expected_secret) in test_vectors {
            let account = parse(url).expect("failed to parse steam url");
            assert_eq!(account.code_type, CodeType::STEAM);
            assert_eq!(account.issuer.to_lowercase(), "steam");
            assert_eq!(account.name, expected_name);
            assert_eq!(account.secret, expected_secret);
        }
    }
}