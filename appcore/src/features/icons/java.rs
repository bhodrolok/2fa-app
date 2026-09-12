use std::{
    collections::HashMap,
    fs::{read, read_to_string},
    path::PathBuf,
};

use jni::{
    EnvUnowned, JValue,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JIntArray, JString},
    sys::{jboolean, jint, jlong, jobject, jstring},
};

use crate::{CLIENT, features::icons::schema::IconPack};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1icons_1slot0_1timestamp_1get(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jlong {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let directory = {
            let client = CLIENT.lock().unwrap();
            PathBuf::from(&client.application_directory)
        };

        let path = directory.join("no_backup/icons/0/metadata.json");

        let json = match read_to_string(&path) {
            Ok(json) => json,
            Err(_) => return Ok(0),
        };

        let metadata = match serde_json::from_str::<serde_json::Value>(&json) {
            Ok(metadata) => metadata,
            Err(_) => return Ok(0),
        };

        Ok(metadata
            .get("timestamp")
            .and_then(|timestamp| timestamp.as_i64())
            .unwrap_or(0))
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1icons_1slot_1get(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jint {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let client = CLIENT.lock().unwrap();

        Ok(client.slot as jint)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1icons_1name_1get(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let client = CLIENT.lock().unwrap();

        Ok(env.new_string(client.name.clone()).unwrap().into_raw())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1icons_1timestamp_1get(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jlong {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let client = CLIENT.lock().unwrap();

        Ok(client.timestamp as jlong)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1icons_1map_1bitmap_1get(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    issuer: JString,
    size: jint,
) -> jobject {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let null = std::ptr::null_mut();

        if size <= 0 {
            return Ok(null);
        }

        let path = {
            let issuer = issuer.to_string().to_lowercase();
            let client = CLIENT.lock().unwrap();

            let Some((file, _)) = client.map.get(&issuer).filter(|(file, _)| !file.is_empty())
            else {
                return Ok(null);
            };

            PathBuf::from(&client.application_directory)
                .join(format!("no_backup/icons/{}/assets/{}", client.slot, file))
        };

        let Ok(data) = read(&path) else {
            return Ok(null);
        };

        let Ok(tree) = resvg::usvg::Tree::from_data(&data, &resvg::usvg::Options::default()) else {
            return Ok(null);
        };

        let size = size as u32;

        let Some(mut pixmap) = resvg::tiny_skia::Pixmap::new(size, size) else {
            return Ok(null);
        };

        let scale = size as f32 / tree.size().width().max(tree.size().height());

        resvg::render(
            &tree,
            resvg::tiny_skia::Transform::from_scale(scale, scale),
            &mut pixmap.as_mut(),
        );

        let pixels: Vec<jint> = pixmap
            .pixels()
            .iter()
            .map(|pixel| {
                let pixel = pixel.demultiply();

                ((pixel.alpha() as i32) << 24)
                    | ((pixel.red() as i32) << 16)
                    | ((pixel.green() as i32) << 8)
                    | (pixel.blue() as i32)
            })
            .collect();

        let array = JIntArray::from(env.new_int_array(pixels.len())?);
        array.set_region(env, 0, &pixels)?;

        let config = env
            .get_static_field(
                jni::jni_str!("android/graphics/Bitmap$Config"),
                jni::jni_str!("ARGB_8888"),
                jni::jni_sig!("Landroid/graphics/Bitmap$Config;"),
            )
            .unwrap()
            .l()
            .unwrap();

        let bitmap = env
            .call_static_method(
                jni::jni_str!("android/graphics/Bitmap"),
                jni::jni_str!("createBitmap"),
                jni::jni_sig!("([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"),
                &[
                    JValue::Object(&array),
                    JValue::Int(size as i32),
                    JValue::Int(size as i32),
                    JValue::Object(&config),
                ],
            )
            .unwrap()
            .l()
            .unwrap();

        Ok(bitmap.into_raw())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1icons_1map_1color_1get(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    issuer: JString,
) -> jint {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let issuer = issuer.to_string().to_lowercase();

        let client = CLIENT.lock().unwrap();

        Ok(match client.map.get(&issuer) {
            Some((_, color)) => *color as jint,
            None => 0,
        })
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1icons_1load(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    skip_and_clear_map: jboolean,
    textured: jboolean,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let directory = {
            let client = CLIENT.lock().unwrap();
            PathBuf::from(&client.application_directory)
        };

        let pack = [1, 0].iter().find_map(|slot| {
            let path = directory.join(format!("no_backup/icons/{}/metadata.json", slot));
            let json = read_to_string(&path).ok()?;
            let pack = serde_json::from_str::<IconPack>(&json).ok()?;
            Some((*slot as u32, pack))
        });

        let Some((slot, pack)) = pack else {
            return Ok(());
        };

        if skip_and_clear_map {
            let mut client = CLIENT.lock().unwrap();

            client.slot = slot;
            client.name = pack.name;
            client.timestamp = pack.timestamp;
            client.map.clear();

            return Ok(());
        }

        let mut map = HashMap::with_capacity(pack.icons.len());

        for entry in pack.icons {
            let file = if textured {
                entry.textured
            } else {
                entry.tinted
            }
            .unwrap_or_default();

            let color = u32::from_str_radix(&entry.color, 16).unwrap_or(0xFF808080);

            for issuer in &entry.issuer {
                map.insert(issuer.to_lowercase(), (file.clone(), color));
            }
        }

        let mut client = CLIENT.lock().unwrap();

        client.slot = slot;
        client.name = pack.name;
        client.timestamp = pack.timestamp;
        client.map = map;

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}
