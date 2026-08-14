pub fn uuid_to_u64(uuid: &str) -> Option<u64> {
    if uuid.len() < 36 {
        return None;
    }

    match (
        u64::from_str_radix(&uuid[0..8], 16),
        u64::from_str_radix(&uuid[28..36], 16),
    ) {
        (Ok(high), Ok(low)) => Some((high << 32) | low),
        _ => None,
    }
}

pub fn u64_to_uuid(id: u64) -> String {
    let high = (id >> 32) as u32;
    let low = (id & 0xFFFF_FFFF) as u32;
    format!("{high:08x}-0000-4000-8000-0000{low:08x}")
}
