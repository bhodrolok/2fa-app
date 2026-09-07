use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JIntArray, JString},
    sys::{jboolean, jint},
};
use zeroize::{Zeroize, Zeroizing};

use crate::{CLIENT, features::otpauth::deserialize::deserialize};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1otpauth_1import_1uri_1into_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    uri: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let uri = Zeroizing::new(uri.to_string());

        if let Some(mut account) = deserialize(&uri, true) {
            let mut client = CLIENT.lock().unwrap();

            account.color = match client.map.get(&account.issuer.to_lowercase()) {
                Some((_, color)) => *color,
                None => 0xFF808080,
            };

            client.hostages.zeroize();
            client.hostages.push(account);

            return Ok(true);
        }

        Ok(false)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1otpauth_1import_1mafile_1into_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    json: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let json = Zeroizing::new(json.to_string());

        let Ok(json) = serde_json::from_str::<serde_json::Value>(&json) else {
            return Ok(false);
        };

        let Some(uri) = json.get("uri").and_then(|v| v.as_str()) else {
            return Ok(false);
        };

        let Some(mut account) = deserialize(uri, true) else {
            return Ok(false);
        };

        let mut client = CLIENT.lock().unwrap();

        account.color = match client.map.get(&account.issuer.to_lowercase()) {
            Some((_, color)) => *color,
            None => 0xFF808080,
        };

        client.hostages.zeroize();
        client.hostages.push(account);

        Ok(true)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1otpauth_1import_1qrcode_1into_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    width: jint,
    height: jint,
    pixels: JIntArray,
) -> jboolean {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let (width, height) = (width as usize, height as usize);

        let mut argb = Zeroizing::new(vec![0i32; width * height]);
        pixels.get_region(env, 0, &mut argb).unwrap();

        let mut image = rqrr::PreparedImage::prepare_from_greyscale(width, height, |x, y| {
            let p = argb[y * width + x];
            let r = (p >> 16 & 0xFF) as u32;
            let g = (p >> 8 & 0xFF) as u32;
            let b = (p & 0xFF) as u32;
            ((r * 299 + g * 587 + b * 114) / 1000) as u8
        });

        for grid in image.detect_grids() {
            if let Ok((_, content)) = grid.decode() {
                let content = Zeroizing::new(content);

                if let Some(mut account) = deserialize(&content, true) {
                    let mut client = CLIENT.lock().unwrap();

                    account.color = match client.map.get(&account.issuer.to_lowercase()) {
                        Some((_, color)) => *color,
                        None => 0xFF808080,
                    };

                    client.hostages.zeroize();
                    client.hostages.push(account);

                    return Ok(true);
                }
            }
        }

        Ok(false)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}
