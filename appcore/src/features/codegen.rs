use hmac::{Hmac, Mac};
use sha1::Sha1;
use sha2::{Sha256, Sha512};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CodeType {
    HOTP,
    TOTP,
    STEAM,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Algorithm {
    SHA1,
    SHA256,
    SHA512,
}

// https://www.rfc-editor.org/rfc/rfc4226
pub fn hotp(secret: &[u8], algorithm: Algorithm, digits: u32, counter: u64) -> String {
    let counter_bytes = counter.to_be_bytes();

    let result = match algorithm {
        Algorithm::SHA1 => {
            let mut mac = Hmac::<Sha1>::new_from_slice(secret).unwrap();
            mac.update(&counter_bytes);
            mac.finalize().into_bytes().to_vec()
        }
        Algorithm::SHA256 => {
            let mut mac = Hmac::<Sha256>::new_from_slice(secret).unwrap();
            mac.update(&counter_bytes);
            mac.finalize().into_bytes().to_vec()
        }
        Algorithm::SHA512 => {
            let mut mac = Hmac::<Sha512>::new_from_slice(secret).unwrap();
            mac.update(&counter_bytes);
            mac.finalize().into_bytes().to_vec()
        }
    };

    let offset = (result[result.len() - 1] & 0x0f) as usize;
    let truncated = u32::from_be_bytes([
        result[offset],
        result[offset + 1],
        result[offset + 2],
        result[offset + 3],
    ]) & 0x7fffffff;

    let modulus = 10u64.pow(digits);
    let code = (truncated as u64 % modulus) as u64;

    format!("{:0width$}", code, width = digits as usize)
}

// https://www.rfc-editor.org/rfc/rfc6238
pub fn totp_custom(
    secret: &[u8],
    algorithm: Algorithm,
    digits: u32,
    timestamp: u64,
    period: u64,
) -> String {
    let counter = timestamp / period;
    hotp(secret, algorithm, digits, counter)
}

pub fn totp(secret: &[u8], algorithm: Algorithm, digits: u32, period: u64) -> String {
    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs();

    totp_custom(secret, algorithm, digits, now, period)
}

const STEAM_ALPHABET: &[u8] = b"23456789BCDFGHJKMNPQRTVWXY";

// https://github.com/Sporoid/SteamAuthenticator
// https://github.com/Jessecar96/SteamDesktopAuthenticator
pub fn steam_custom(secret: &[u8], timestamp: u64) -> String {
    let period = 30u64;
    let counter = timestamp / period;
    let counter_bytes = counter.to_be_bytes();

    let mut mac = Hmac::<Sha1>::new_from_slice(secret).unwrap();
    mac.update(&counter_bytes);
    let result = mac.finalize().into_bytes();

    let offset = (result[result.len() - 1] & 0x0f) as usize;
    let mut code = u32::from_be_bytes([
        result[offset],
        result[offset + 1],
        result[offset + 2],
        result[offset + 3],
    ]) & 0x7fffffff;

    let mut chars = ['\0'; 5];
    for i in 0..5 {
        chars[i] = STEAM_ALPHABET[(code % 26) as usize] as char;
        code /= 26;
    }

    chars.iter().collect()
}

pub fn steam(secret: &[u8]) -> String {
    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs();

    steam_custom(secret, now)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    // https://www.rfc-editor.org/rfc/rfc4226#appendix-D
    fn test_hotp() {
        let secret = b"12345678901234567890";

        let test_vectors = vec![
            // (counter, expected_code)
            (0, "755224"),
            (1, "287082"),
            (2, "359152"),
            (3, "969429"),
            (4, "338314"),
            (5, "254676"),
            (6, "287922"),
            (7, "162583"),
            (8, "399871"),
            (9, "520489"),
        ];

        for (counter, expected_code) in test_vectors {
            let code = hotp(secret, Algorithm::SHA1, 6, counter);
            assert_eq!(
                code, expected_code,
                "counter {}: expected {}, got {}",
                counter, expected_code, code
            )
        }
    }

    #[test]
    // https://www.rfc-editor.org/rfc/rfc6238#appendix-B
    fn test_totp() {
        let secret_sha1 = b"12345678901234567890";
        let secret_sha256 = b"12345678901234567890123456789012";
        let secret_sha512 = b"1234567890123456789012345678901234567890123456789012345678901234";

        let test_vectors = vec![
            // (timestamp, sha1, sha256, sha512)
            (59u64, "94287082", "46119246", "90693936"),
            (1111111109u64, "07081804", "68084774", "25091201"),
            (1111111111u64, "14050471", "67062674", "99943326"),
            (1234567890u64, "89005924", "91819424", "93441116"),
            (2000000000u64, "69279037", "90698825", "38618901"),
            (20000000000u64, "65353130", "77737706", "47863826"),
        ];

        for (timestamp, expected_sha1, expected_sha256, expected_sha512) in test_vectors {
            assert_eq!(
                totp_custom(secret_sha1, Algorithm::SHA1, 8, timestamp, 30),
                expected_sha1,
                "SHA1 failed at timestamp {}",
                timestamp
            );

            assert_eq!(
                totp_custom(secret_sha256, Algorithm::SHA256, 8, timestamp, 30),
                expected_sha256,
                "SHA256 failed at timestamp {}",
                timestamp
            );

            assert_eq!(
                totp_custom(secret_sha512, Algorithm::SHA512, 8, timestamp, 30),
                expected_sha512,
                "SHA512 failed at timestamp {}",
                timestamp
            );
        }
    }

    #[test]
    // https://github.com/Sporoid/SteamAuthenticator
    fn test_steam() {
        let test_cases = vec![
            // (secret_b32, timestamp, expected_code)
            ("MQ3XOP2I75XORCQIEKT63DMOIU2WDT7F", 1766747555, "PBV8F"),
            ("MQ3XOP2I75XORCQIEKT63DMOIU2WDT7F", 1766747598, "T2VYN"),
            ("ETPBCTYOI34ACGK7CAXBBJUUCAKUH2I2", 1766747661, "YX329"),
            ("ETPBCTYOI34ACGK7CAXBBJUUCAKUH2I2", 1766747676, "GB86X"),
            ("R7RDTIMSRCVE7CS7DWEVKHOVRBLFYXWC", 1766747676, "8PF3Q"),
            ("R7RDTIMSRCVE7CS7DWEVKHOVRBLFYXWC", 1766747701, "J5JH2"),
        ];

        for (secret_b32, timestamp, expected_code) in test_cases {
            let secret = data_encoding::BASE32_NOPAD
                .decode(secret_b32.as_bytes())
                .unwrap();
            let code = steam_custom(&secret, timestamp);

            assert_eq!(
                code, expected_code,
                "timestamp {}: expected {}, got {}",
                timestamp, expected_code, code
            );
        }
    }
}
