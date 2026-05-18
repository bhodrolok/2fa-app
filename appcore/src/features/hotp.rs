use hmac::{Hmac, KeyInit, Mac};
use sha1::Sha1;
use sha2::{Sha256, Sha512};

use crate::utils::types::Algorithm;

pub fn generate(secret: &[u8], algorithm: Algorithm, digits: u32, counter: u64) -> String {
    assert!((4..=10).contains(&digits), "hotp invalid digits={}", digits);
    let counter_bytes = counter.to_be_bytes();

    let mut buf = [0u8; 64];
    let hmac_result: &[u8] = match algorithm {
        Algorithm::SHA1 => {
            let mut mac = Hmac::<Sha1>::new_from_slice(secret).unwrap();
            mac.update(&counter_bytes);
            let out = mac.finalize().into_bytes();
            buf[..out.len()].copy_from_slice(&out);
            &buf[..out.len()]
        }
        Algorithm::SHA256 => {
            let mut mac = Hmac::<Sha256>::new_from_slice(secret).unwrap();
            mac.update(&counter_bytes);
            let out = mac.finalize().into_bytes();
            buf[..out.len()].copy_from_slice(&out);
            &buf[..out.len()]
        }
        Algorithm::SHA512 => {
            let mut mac = Hmac::<Sha512>::new_from_slice(secret).unwrap();
            mac.update(&counter_bytes);
            let out = mac.finalize().into_bytes();
            buf[..out.len()].copy_from_slice(&out);
            &buf[..out.len()]
        }
    };

    let offset = (hmac_result[hmac_result.len() - 1] & 0x0f) as usize;
    let truncated = u32::from_be_bytes([
        hmac_result[offset],
        hmac_result[offset + 1],
        hmac_result[offset + 2],
        hmac_result[offset + 3],
    ]) & 0x7fffffff;

    let modulus = 10u64.pow(digits);
    let code = (truncated as u64) % modulus;

    let width = digits as usize;
    format!("{code:0width$}")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_rfc4226_appendix_d_vectors() {
        // https://www.rfc-editor.org/rfc/rfc4226

        let vectors: &[(u64, &str)] = &[
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

        for &(counter, hotp) in vectors {
            assert_eq!(
                generate(b"12345678901234567890", Algorithm::SHA1, 6, counter),
                hotp,
                "counter={counter}"
            );
        }
    }

    #[test]
    fn test_rfc6238_appendix_b_vectors() {
        // https://www.rfc-editor.org/rfc/rfc6238

        let vectors: &[(u64, &str, Algorithm)] = &[
            (59, "94287082", Algorithm::SHA1),
            (59, "46119246", Algorithm::SHA256),
            (59, "90693936", Algorithm::SHA512),
            (1111111109, "07081804", Algorithm::SHA1),
            (1111111109, "68084774", Algorithm::SHA256),
            (1111111109, "25091201", Algorithm::SHA512),
            (1111111111, "14050471", Algorithm::SHA1),
            (1111111111, "67062674", Algorithm::SHA256),
            (1111111111, "99943326", Algorithm::SHA512),
            (1234567890, "89005924", Algorithm::SHA1),
            (1234567890, "91819424", Algorithm::SHA256),
            (1234567890, "93441116", Algorithm::SHA512),
            (2000000000, "69279037", Algorithm::SHA1),
            (2000000000, "90698825", Algorithm::SHA256),
            (2000000000, "38618901", Algorithm::SHA512),
            (20000000000, "65353130", Algorithm::SHA1),
            (20000000000, "77737706", Algorithm::SHA256),
            (20000000000, "47863826", Algorithm::SHA512),
        ];

        for &(time, totp, algorithm) in vectors {
            let secret: &[u8] = match algorithm {
                Algorithm::SHA1 => b"12345678901234567890",
                Algorithm::SHA256 => b"12345678901234567890123456789012",
                Algorithm::SHA512 => {
                    b"1234567890123456789012345678901234567890123456789012345678901234"
                }
            };

            let counter = time / 30;

            assert_eq!(
                generate(secret, algorithm, 8, counter),
                totp,
                "time={time} algorithm={algorithm:?}"
            );
        }
    }

    #[test]
    fn test_four_digits() {
        let vectors: &[(u64, &str, Algorithm)] = &[
            (59, "7082", Algorithm::SHA1),
            (59, "9246", Algorithm::SHA256),
            (59, "3936", Algorithm::SHA512),
            (1111111109, "1804", Algorithm::SHA1),
            (1111111109, "4774", Algorithm::SHA256),
            (1111111109, "1201", Algorithm::SHA512),
            (1111111111, "0471", Algorithm::SHA1),
            (1111111111, "2674", Algorithm::SHA256),
            (1111111111, "3326", Algorithm::SHA512),
            (1234567890, "5924", Algorithm::SHA1),
            (1234567890, "9424", Algorithm::SHA256),
            (1234567890, "1116", Algorithm::SHA512),
            (2000000000, "9037", Algorithm::SHA1),
            (2000000000, "8825", Algorithm::SHA256),
            (2000000000, "8901", Algorithm::SHA512),
            (20000000000, "3130", Algorithm::SHA1),
            (20000000000, "7706", Algorithm::SHA256),
            (20000000000, "3826", Algorithm::SHA512),
        ];

        for &(time, totp, algorithm) in vectors {
            let secret: &[u8] = match algorithm {
                Algorithm::SHA1 => b"12345678901234567890",
                Algorithm::SHA256 => b"12345678901234567890123456789012",
                Algorithm::SHA512 => {
                    b"1234567890123456789012345678901234567890123456789012345678901234"
                }
            };

            let counter = time / 30;

            assert_eq!(
                generate(secret, algorithm, 4, counter),
                totp,
                "time={time} algorithm={algorithm:?}"
            );
        }
    }

    #[test]
    fn test_ten_digits() {
        let vectors: &[(u64, &str, Algorithm)] = &[
            (59, "1094287082", Algorithm::SHA1),
            (59, "0746119246", Algorithm::SHA256),
            (59, "0490693936", Algorithm::SHA512),
            (1111111109, "0907081804", Algorithm::SHA1),
            (1111111109, "1568084774", Algorithm::SHA256),
            (1111111109, "0225091201", Algorithm::SHA512),
            (1111111111, "0414050471", Algorithm::SHA1),
            (1111111111, "1167062674", Algorithm::SHA256),
            (1111111111, "1899943326", Algorithm::SHA512),
            (1234567890, "0689005924", Algorithm::SHA1),
            (1234567890, "0091819424", Algorithm::SHA256),
            (1234567890, "1493441116", Algorithm::SHA512),
            (2000000000, "2069279037", Algorithm::SHA1),
            (2000000000, "1790698825", Algorithm::SHA256),
            (2000000000, "1938618901", Algorithm::SHA512),
            (20000000000, "1465353130", Algorithm::SHA1),
            (20000000000, "0777737706", Algorithm::SHA256),
            (20000000000, "1047863826", Algorithm::SHA512),
        ];

        for &(time, totp, algorithm) in vectors {
            let secret: &[u8] = match algorithm {
                Algorithm::SHA1 => b"12345678901234567890",
                Algorithm::SHA256 => b"12345678901234567890123456789012",
                Algorithm::SHA512 => {
                    b"1234567890123456789012345678901234567890123456789012345678901234"
                }
            };

            let counter = time / 30;

            assert_eq!(
                generate(secret, algorithm, 10, counter),
                totp,
                "time={time} algorithm={algorithm:?}"
            );
        }
    }
}
