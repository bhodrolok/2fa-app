use crate::features::otp::{algorithm::Algorithm, hotp};

pub fn totp(
    secret: &[u8],
    algorithm: Algorithm,
    digits: u32,
    timestamp: u64,
    period: u64,
) -> String {
    let counter = timestamp / period;

    hotp::generate(secret, algorithm, digits, counter)
}
