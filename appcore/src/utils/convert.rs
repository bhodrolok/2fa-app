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

pub fn uuid_to_id(uuid: &str) -> Option<u64> {
    let hex_str: String = uuid.chars().filter(|c| *c != '-').collect();
    let bytes = data_encoding::HEXLOWER_PERMISSIVE
        .decode(hex_str.as_bytes())
        .ok()?;

    if bytes.len() != 16 {
        return None;
    }

    let mut id_bytes = [0u8; 8];
    id_bytes.copy_from_slice(&bytes[..8]);
    Some(u64::from_be_bytes(id_bytes))
}

pub fn id_to_uuid(id: u64) -> String {
    let mut bytes = [0u8; 16];
    bytes[..8].copy_from_slice(&id.to_be_bytes());

    let hex = data_encoding::HEXLOWER_PERMISSIVE.encode(&bytes);
    format!(
        "{}-{}-{}-{}-{}",
        &hex[0..8],
        &hex[8..12],
        &hex[12..16],
        &hex[16..20],
        &hex[20..32]
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_id_uuid_roundtrip() {
        let id = 0x123456789abcdefu64;
        let uuid = id_to_uuid(id);
        let result = uuid_to_id(&uuid).unwrap();
        assert_eq!(id, result);
    }
}
