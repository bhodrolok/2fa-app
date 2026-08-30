use hmac::{Hmac, KeyInit, Mac};
use sha1::Sha1;
use zeroize::Zeroize;

const STEAM_ALPHABET: &[u8] = b"23456789BCDFGHJKMNPQRTVWXY";

// https://github.com/Sporoid/SteamAuthenticator
// https://github.com/Jessecar96/SteamDesktopAuthenticator
pub fn generate(secret: &[u8], timestamp: u64) -> String {
    let period = 30u64;
    let counter = timestamp / period;
    let counter_bytes = counter.to_be_bytes();

    let mut mac = Hmac::<Sha1>::new_from_slice(secret).unwrap();
    mac.update(&counter_bytes);
    let result = mac.finalize().into_bytes();

    let offset = (result[result.len() - 1] & 0x0f) as usize;
    let mut code = u32::from_be_bytes(result[offset..offset + 4].try_into().unwrap()) & 0x7fffffff;

    let mut chars = ['\0'; 5];
    for i in 0..5 {
        chars[i] = STEAM_ALPHABET[(code % 26) as usize] as char;
        code /= 26;
    }

    code.zeroize();

    chars.iter().collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_steam() {
        // https://github.com/Sporoid/SteamAuthenticator

        let test_cases = vec![
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
            let code = generate(&secret, timestamp);

            assert_eq!(
                code, expected_code,
                "timestamp={timestamp} secret_b32={secret_b32:?}"
            );
        }
    }
}
