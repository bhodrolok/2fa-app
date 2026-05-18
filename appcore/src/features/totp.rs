use crate::{features::hotp, utils::types::Algorithm};

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
