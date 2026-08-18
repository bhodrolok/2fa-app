use zeroize::Zeroize;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Zeroize)]
#[repr(i32)]
pub enum CodeType {
    HOTP = 0,
    TOTP = 1,
    STEAM = 2,
}
