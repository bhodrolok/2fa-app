use zeroize::Zeroize;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Zeroize)]
pub enum Algorithm {
    SHA1,
    SHA256,
    SHA512,
}
