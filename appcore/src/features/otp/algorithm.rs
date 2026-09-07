use zeroize::Zeroize;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Zeroize)]
#[repr(i32)]
pub enum Algorithm {
    SHA1 = 0,
    SHA256 = 1,
    SHA512 = 2,
}
