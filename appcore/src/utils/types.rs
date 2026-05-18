#[derive(Debug, Clone, Copy, PartialEq, Eq, zeroize::Zeroize)]
pub enum CodeType {
    HOTP,
    TOTP,
    STEAM,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, zeroize::Zeroize)]
pub enum Algorithm {
    SHA1,
    SHA256,
    SHA512,
}
