pub fn generate_account_color() -> u32 {
    const GRAY: u32 = 0xFFCACACA;
    const RED: u32 = 0xFFFF2A2A;
    const GREEN: u32 = 0xFF3BFF3B;
    const BLUE: u32 = 0xFF2B8BFF;
    const YELLOW: u32 = 0xFFFFD700;
    const CYAN: u32 = 0xFF2DE6E6;
    const PINK: u32 = 0xFFFF2EDC;

    const COLORS: [u32; 7] = [GRAY, RED, GREEN, BLUE, YELLOW, CYAN, PINK];

    let seed = getrandom::u32().unwrap_or(0);
    let index = (seed % COLORS.len() as u32) as usize;

    COLORS[index]
}
