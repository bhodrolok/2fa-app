pub fn uuid_to_u64(uuid: &str) -> Option<u64> {
    let high = u64::from_str_radix(uuid.get(0..8)?, 16).ok()?;
    let low = u64::from_str_radix(uuid.get(28..36)?, 16).ok()?;

    Some((high << 32) | low)
}

pub fn u64_to_uuid(id: u64) -> String {
    let high = (id >> 32) as u32;
    let low = (id & 0xFFFFFFFF) as u32;
    format!("{high:08x}-0000-4000-8000-0000{low:08x}")
}
