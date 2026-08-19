use jni::{
    EnvUnowned, JValue,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JString},
    strings::JNIString,
    sys::{jboolean, jfloat, jint, jlong, jobject, jstring},
};

use crate::{
    CLIENT,
    features::otp::{algorithm::Algorithm, r#type::CodeType},
    utils::color::{hsl_to_rgb::hsl_to_rgb, rgb_to_hsl::rgb_to_hsl},
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1get(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jobject {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let client = CLIENT.lock().unwrap();

        let Some(account) = &client.edit else {
            env.fatal_error(&JNIString::from("features_edit_get called with edit none"))
        };

        let id = JValue::Long(account.id as jlong);
        let name_jstr = env.new_string(&account.name)?;
        let name = JValue::Object(&name_jstr);
        let issuer_jstr = env.new_string(&account.issuer)?;
        let issuer = JValue::Object(&issuer_jstr);
        let note_jstr = env.new_string(&account.note)?;
        let note = JValue::Object(&note_jstr);
        let secret_base32 = data_encoding::BASE32_NOPAD.encode(&account.secret);
        let secret_jstr = env.new_string(secret_base32)?;
        let secret = JValue::Object(&secret_jstr);
        let code_type = JValue::Int(account.code_type as jint);
        let algorithm = JValue::Int(account.algorithm as jint);
        let counter = JValue::Long(account.counter as jlong);
        let period = JValue::Long(account.period as jlong);
        let digits = JValue::Int(account.digits as jint);
        let color = JValue::Int(account.color as jint);

        let account_obj = match env.new_object(
            jni::jni_str!("app/ninesevennine/twofactorauthenticator/Account"),
            jni::jni_sig!(
                "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIJJII)V"
            ),
            &[
                id, name, issuer, note, secret, code_type, algorithm, counter, period, digits,
                color,
            ],
        ) {
            Ok(obj) => obj,
            Err(e) => env.fatal_error(&JNIString::from(e.to_string())),
        };

        Ok(account_obj.into_raw())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1color_1get(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jint {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let client = CLIENT.lock().unwrap();

        let color = match &client.edit {
            Some(account) => account.color,
            None => 0xFF808080,
        };

        Ok(color as jint)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1color_1get_1as_1hex(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let client = CLIENT.lock().unwrap();

        let hex = match &client.edit {
            Some(account) => format!("#{:08x}", account.color),
            None => String::new(),
        };

        Ok(env.new_string(hex).unwrap().into_raw())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1color_1set__I(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    argb: jint,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            let (h, s, _) = rgb_to_hsl(argb as u32);
            let color = hsl_to_rgb(h, s, 0.5);
            account.color = color as u32;
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1color_1hue_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    hue: jfloat,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            let (_, s, _) = rgb_to_hsl(account.color);
            let color = hsl_to_rgb(hue, s, 0.5);
            account.color = color;
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1color_1saturation_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    saturation: jfloat,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            let (h, _, _) = rgb_to_hsl(account.color);
            let color = hsl_to_rgb(h, saturation, 0.5);
            account.color = color;
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1color_1set__Ljava_lang_String_2(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    argb: JString,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let argb = argb.to_string();

        let color = match u32::from_str_radix(argb.trim().trim_start_matches('#'), 16) {
            Ok(c) => c & 0x00FFFFFF,
            Err(_) => return Ok(()),
        };

        let (h, s, _) = rgb_to_hsl(color);
        let color = hsl_to_rgb(h, s, 0.5);

        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.color = color;
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1name_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    name: JString,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let name = name.to_string();

        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.name = name.trim().to_string();
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1issuer_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    issuer: JString,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let issuer = issuer.to_string();

        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.issuer = issuer.trim().to_string();
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1note_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    note: JString,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let note = note.to_string();

        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.note = note.trim().to_string();
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1secret_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    secret_base32_nopad: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let secret_base32_nopad = secret_base32_nopad.to_string();

        let secret = match data_encoding::BASE32_NOPAD.decode(secret_base32_nopad.trim().as_bytes())
        {
            Ok(s) => s,
            Err(_) => return Ok(false),
        };

        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.secret = secret;
        }

        Ok(true)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1type_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    code_type: jint,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let code_type = match code_type {
            0 => CodeType::HOTP,
            1 => CodeType::TOTP,
            2 => CodeType::STEAM,
            _ => return Ok(()),
        };

        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.code_type = code_type
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1algorithm_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    algorithm: jint,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let algorithm = match algorithm {
            0 => Algorithm::SHA1,
            1 => Algorithm::SHA256,
            2 => Algorithm::SHA512,
            _ => return Ok(()),
        };

        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.algorithm = algorithm
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1counter_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    counter: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let counter = counter.to_string();

        let counter = match counter.trim().parse() {
            Ok(c) => c,
            Err(_) => return Ok(false),
        };

        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.counter = counter
        }

        Ok(true)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1period_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    period: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let period = period.to_string();

        let period = match period.trim().parse() {
            Ok(p) => p,
            Err(_) => return Ok(false),
        };

        if period < 10 {
            return Ok(false);
        }

        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.period = period
        }

        Ok(true)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1digits_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    digits: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let digits = digits.to_string();

        let digits = match digits.trim().parse() {
            Ok(d) => d,
            Err(_) => return Ok(false),
        };

        if !(4..=10).contains(&digits) {
            return Ok(false);
        }

        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.digits = digits
        }

        Ok(true)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1edit_1delete(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    id: jlong,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        client.accounts.retain(|a| a.id != id as u64);
        client.edit = None;

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}
