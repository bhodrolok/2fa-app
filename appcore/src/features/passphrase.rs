pub fn estimate_strength(passphrase: &str) -> f32 {
    let estimate = zxcvbn::zxcvbn(&passphrase, &[]);

    let min_log = 3.0;
    let max_log = 18.0;

    ((estimate.guesses_log10() - min_log) / (max_log - min_log)).clamp(0.0, 1.0) as f32
}
