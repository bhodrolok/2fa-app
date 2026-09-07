#[derive(Debug, Copy, Clone, PartialEq, Eq)]
#[repr(i32)]
pub enum ImportResult {
    Success = 0,
    InvalidFormat = -1,
    UnsupportedVersion = -2,
    IncorrectPassphrase = -3,
}
