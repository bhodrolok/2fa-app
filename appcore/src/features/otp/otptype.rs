use zeroize::Zeroize;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Zeroize)]
pub enum CodeType {
    HOTP,
    TOTP,
    STEAM,
}
