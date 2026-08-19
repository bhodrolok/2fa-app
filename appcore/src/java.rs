use crate::{
    _client::CLIENT_UNLOCK_PASSPHRASE_HASH_LEN,
    CLIENT,
    features::{self},
};
use jni::{
    EnvUnowned,
    objects::{JByteArray, JClass, JIntArray, JString, JValue},
    sys::{JNI_FALSE, JNI_TRUE, jboolean, jbyteArray, jint, jobject, jstring},
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_clientUnload(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        client.unload();

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setUnlockPassphraseHash(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    hash: JByteArray,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let len = hash.len(env).unwrap();
        let mut secret_vec = vec![0i8; len];

        hash.get_region(env, 0, &mut secret_vec).unwrap();

        let bytes: Vec<u8> = secret_vec.iter().map(|b| *b as u8).collect();

        let mut hash = [0u8; CLIENT_UNLOCK_PASSPHRASE_HASH_LEN];
        hash.copy_from_slice(&bytes);

        let mut client = CLIENT.lock().unwrap();
        client.set_unlock_passphrase_hash(&hash);

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_changeUnlockPassphraseHash(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    salt: JByteArray,
    passphrase: JString,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let salt = env.convert_byte_array(&salt).unwrap();
        let passphrase: String = passphrase.to_string();

        let mut client = CLIENT.lock().unwrap();
        client.change_unlock_passphrase_hash(&salt, &passphrase);

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_getUnlockPassphraseHash(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jbyteArray {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();
        let uph = client.get_unlock_passphrase_hash();

        let output = env.new_byte_array(uph.len()).unwrap();

        let uph_i8: Vec<i8> = uph.iter().map(|b| *b as i8).collect();
        output.set_region(env, 0, &uph_i8).unwrap();

        Ok(output.into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_isClientUnlockPassphraseHashEmpty(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();
        let unlock_passphrase_hash = client.get_unlock_passphrase_hash();

        if unlock_passphrase_hash.iter().all(|b| *b == 0) {
            Ok(JNI_TRUE)
        } else {
            Ok(JNI_FALSE)
        }
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountEmpty(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        client.set_edit_account_empty();

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountFromOtpAuth(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    url: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let url: String = url.to_string();
        let mut client = CLIENT.lock().unwrap();

        if client.set_edit_account_from_otpauth(&url) {
            Ok(JNI_TRUE)
        } else {
            Ok(JNI_FALSE)
        }
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_isEditAccountSome(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();
        if client.edit.is_some() {
            Ok(JNI_TRUE)
        } else {
            Ok(JNI_FALSE)
        }
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_exportAccountsUsingUnlockPassphraseHash(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    salt: JByteArray,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let salt = env.convert_byte_array(&salt).unwrap();

        let client = CLIENT.lock().unwrap();

        Ok(env
            .new_string(client.export_accounts_using_unlock_passphrase_hash(&salt))
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_exportAccounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let passphrase: String = passphrase.to_string();
        let client = CLIENT.lock().unwrap();

        Ok(env
            .new_string(client.export_accounts(&passphrase))
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_importAccountsUsingUnlockPassphraseHash(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    data: JString,
) -> jint {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let data: String = data.to_string();

        let mut client = CLIENT.lock().unwrap();

        Ok(client.import_accounts_using_unlock_passphrase_hash(&data))
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_importAccounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
    data: JString,
) -> jint {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let passphrase: String = passphrase.to_string();
        let data: String = data.to_string();

        let mut client = CLIENT.lock().unwrap();

        Ok(client.import_accounts(&passphrase, &data))
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_svgToBitmap(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    data: JByteArray,
    width: jint,
    height: jint,
) -> jobject {
    use resvg::tiny_skia;
    use resvg::usvg;

    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        if width <= 0 || height <= 0 {
            return Ok(std::ptr::null_mut());
        }

        let len = match data.len(env) {
            Ok(l) => l as usize,
            Err(_) => return Ok(std::ptr::null_mut()),
        };

        let mut buf_i8 = vec![0i8; len];
        if let Err(_) = data.get_region(env, 0, &mut buf_i8) {
            return Ok(std::ptr::null_mut());
        }
        let buf: Vec<u8> = buf_i8.iter().map(|&b| b as u8).collect();

        let opt = usvg::Options::default();
        let rtree = match usvg::Tree::from_data(&buf, &opt) {
            Ok(t) => t,
            Err(_) => return Ok(std::ptr::null_mut()),
        };

        let svg_size = rtree.size();
        let svg_w = svg_size.width();
        let svg_h = svg_size.height();
        if svg_w == 0.0 || svg_h == 0.0 {
            return Ok(std::ptr::null_mut());
        }

        let w = width as u32;
        let h = height as u32;
        let mut pixmap = match tiny_skia::Pixmap::new(w, h) {
            Some(p) => p,
            None => return Ok(std::ptr::null_mut()),
        };

        let sx = (w as f32) / svg_w;
        let sy = (h as f32) / svg_h;

        let transform = tiny_skia::Transform::from_row(sx, 0.0, 0.0, sy, 0.0, 0.0);

        resvg::render(&rtree, transform, &mut pixmap.as_mut());

        let pixels = pixmap.data();

        let mut argb_pixels = Vec::with_capacity((w * h) as usize);
        for chunk in pixels.chunks_exact(4) {
            let r = chunk[0] as i32;
            let g = chunk[1] as i32;
            let b = chunk[2] as i32;
            let a = chunk[3] as i32;
            argb_pixels.push((a << 24) | (r << 16) | (g << 8) | b);
        }

        let bitmap_class = match env.find_class(jni::jni_str!("android/graphics/Bitmap")) {
            Ok(c) => c,
            Err(_) => return Ok(std::ptr::null_mut()),
        };

        let config_class = match env.find_class(jni::jni_str!("android/graphics/Bitmap$Config")) {
            Ok(c) => c,
            Err(_) => return Ok(std::ptr::null_mut()),
        };

        let config_field = match env.get_static_field(
            &config_class,
            jni::jni_str!("ARGB_8888"),
            jni::jni_sig!("Landroid/graphics/Bitmap$Config;"),
        ) {
            Ok(f) => f,
            Err(_) => return Ok(std::ptr::null_mut()),
        };

        let config = match config_field.l() {
            Ok(obj) => obj,
            Err(_) => return Ok(std::ptr::null_mut()),
        };

        if config.is_null() {
            return Ok(std::ptr::null_mut());
        }

        let bitmap_obj = match env.call_static_method(
            &bitmap_class,
            jni::jni_str!("createBitmap"),
            jni::jni_sig!("(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"),
            &[
                JValue::Int(w as i32),
                JValue::Int(h as i32),
                JValue::Object(&config),
            ],
        ) {
            Ok(val) => val,
            Err(_) => return Ok(std::ptr::null_mut()),
        };

        let bitmap = match bitmap_obj.l() {
            Ok(obj) => obj,
            Err(_) => return Ok(std::ptr::null_mut()),
        };

        if bitmap.is_null() {
            return Ok(std::ptr::null_mut());
        }

        let pixel_array = match env.new_int_array(argb_pixels.len()) {
            Ok(arr) => arr,
            Err(_) => return Ok(std::ptr::null_mut()),
        };

        let pixel_array = JIntArray::from(pixel_array);
        if let Err(_) = pixel_array.set_region(env, 0, &argb_pixels) {
            return Ok(std::ptr::null_mut());
        }

        if let Err(_) = env.call_method(
            &bitmap,
            jni::jni_str!("setPixels"),
            jni::jni_sig!("([IIIIIII)V"),
            &[
                JValue::Object(&pixel_array),
                JValue::Int(0),
                JValue::Int(w as i32),
                JValue::Int(0),
                JValue::Int(0),
                JValue::Int(w as i32),
                JValue::Int(h as i32),
            ],
        ) {
            return Ok(std::ptr::null_mut());
        }

        Ok(bitmap.into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_estimatePassphraseStrength(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
) -> f32 {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let passphrase: String = passphrase.to_string();
        Ok(features::passphrase::estimate_strength(passphrase.as_str()))
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
