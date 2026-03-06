use crate::{
    CLIENT,
    client::SECRET_SIZE,
    features::codegen::{Algorithm, CodeType},
};
use jni::{
    EnvUnowned,
    objects::{JByteArray, JClass, JIntArray, JObject, JString, JValue},
    sys::{JNI_FALSE, JNI_TRUE, jboolean, jbyteArray, jint, jlong, jobject, jobjectArray, jstring},
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_generateClientSeedWithEntropy(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    entropy_array: JIntArray,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        use sha3::{Digest, Sha3_512};

        let len = entropy_array.len(env).unwrap();
        let mut entropy_vec = vec![0i32; len];
        entropy_array.get_region(env, 0, &mut entropy_vec).unwrap();

        let entropy_bytes: Vec<u8> = entropy_vec.iter().flat_map(|v| v.to_le_bytes()).collect();

        let mut hasher = Sha3_512::new();
        hasher.update(&entropy_bytes);

        let mut system_random = [0u8; 64];
        getrandom::fill(&mut system_random).unwrap();
        hasher.update(&system_random);

        let mut client = CLIENT.lock().unwrap();
        client.seed = hasher.finalize().into();

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setAutomaticInternalBackupsPass(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    pass: JString,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let pass: String = pass.to_string();

        let mut client = CLIENT.lock().unwrap();
        client.set_automatic_internal_backups_pass(&pass);

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_getSecret(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jbyteArray {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();
        let secret = client.get_secret();

        let output = env.new_byte_array(secret.len()).unwrap();

        let secret_i8: Vec<i8> = secret.iter().map(|b| *b as i8).collect();
        output.set_region(env, 0, &secret_i8).unwrap();

        Ok(output.into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setSecret(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    secret_array: JByteArray,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let len = secret_array.len(env).unwrap();
        let mut secret_vec = vec![0i8; len];

        secret_array.get_region(env, 0, &mut secret_vec).unwrap();

        let secret_bytes: Vec<u8> = secret_vec.iter().map(|b| *b as u8).collect();

        let mut secret = [0u8; SECRET_SIZE];
        secret.copy_from_slice(&secret_bytes);

        let mut client = CLIENT.lock().unwrap();
        client.set_secret(&secret);

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_isSecretEmpty(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();
        let secret = client.get_secret();

        if secret.iter().all(|b| *b == 0) {
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
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountNone(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        client.edit = None;

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
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_copyEditAccountIntoAccounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        client.copy_edit_account_into_accounts();

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_copyAccountIntoEditAccount(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    id: jlong,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        client.copy_account_into_edit_account(id as u64);

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountName(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    name: JString,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let name: String = name.to_string();
        let mut client = CLIENT.lock().unwrap();

        if let Some(ref mut account) = client.edit {
            account.name = name;
        }

        Ok(())
    });
    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountIssuer(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    issuer: JString,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let issuer: String = issuer.to_string();
        let mut client = CLIENT.lock().unwrap();

        if let Some(ref mut account) = client.edit {
            account.issuer = issuer;
        }

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountNote(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    note: JString,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let note: String = note.to_string();
        let mut client = CLIENT.lock().unwrap();

        if let Some(ref mut account) = client.edit {
            account.note = note;
        }

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountSecretBase32NoPad(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    secret_base32_nopad: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let secret_base32_nopad: String = secret_base32_nopad.to_string();
        let secret_vec = match data_encoding::BASE32_NOPAD.decode(secret_base32_nopad.as_bytes()) {
            Ok(v) => v,
            Err(_) => return Ok(JNI_FALSE),
        };

        let mut client = CLIENT.lock().unwrap();
        if let Some(ref mut account) = client.edit {
            account.secret = secret_vec;
        }

        Ok(JNI_TRUE)
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountCodeType(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    code_type_enum: JObject,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let value = env
            .call_method(
                &code_type_enum,
                jni::jni_str!("ordinal"),
                jni::jni_sig!("()I"),
                &[],
            )
            .unwrap()
            .i()
            .unwrap();

        let mut client = CLIENT.lock().unwrap();
        if let Some(ref mut account) = client.edit {
            account.code_type = match value {
                0 => CodeType::HOTP,
                1 => CodeType::TOTP,
                2 => CodeType::STEAM,
                _ => return Ok(()),
            };
        }

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountAlgorithm(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    algorithm_enum: JObject,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let value = env
            .call_method(
                &algorithm_enum,
                jni::jni_str!("ordinal"),
                jni::jni_sig!("()I"),
                &[],
            )
            .unwrap()
            .i()
            .unwrap();

        let mut client = CLIENT.lock().unwrap();
        if let Some(ref mut account) = client.edit {
            account.algorithm = match value {
                0 => Algorithm::SHA1,
                1 => Algorithm::SHA256,
                2 => Algorithm::SHA512,
                _ => return Ok(()),
            };
        }

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountCounter(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    counter: jlong,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        if let Some(ref mut account) = client.edit {
            account.counter = counter as u64;
        }

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountPeriod(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    period: jlong,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        if let Some(ref mut account) = client.edit {
            account.period = period as u64;
        }

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountDigits(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    digits: jint,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        if let Some(ref mut account) = client.edit {
            account.digits = digits as u32;
        }

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountColor(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    color: jint,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        if let Some(ref mut account) = client.edit {
            account.color = color as u32;
        }

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
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
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_getEditAccountWithSecretBase32NoPad(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jobject {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();

        let Some(ref account) = client.edit else {
            return Ok(std::ptr::null_mut());
        };

        let code_type_class = env.find_class(jni::jni_str!(
            "app/ninesevennine/twofactorauthenticator/CodeType"
        )).unwrap();
        let code_type_obj = env
            .get_static_field(
                code_type_class,
                match account.code_type {
                    CodeType::HOTP => jni::jni_str!("HOTP"),
                    CodeType::TOTP => jni::jni_str!("TOTP"),
                    CodeType::STEAM => jni::jni_str!("STEAM"),
                },
                jni::jni_sig!("Lapp/ninesevennine/twofactorauthenticator/CodeType;"),
            ).unwrap()
            .l().unwrap();

        let algorithm_class = env.find_class(jni::jni_str!(
            "app/ninesevennine/twofactorauthenticator/Algorithm"
        )).unwrap();
        let algorithm_obj = env
            .get_static_field(
                algorithm_class,
                match account.algorithm {
                    Algorithm::SHA1 => jni::jni_str!("SHA1"),
                    Algorithm::SHA256 => jni::jni_str!("SHA256"),
                    Algorithm::SHA512 => jni::jni_str!("SHA512"),
                },
                jni::jni_sig!("Lapp/ninesevennine/twofactorauthenticator/Algorithm;"),
            ).unwrap()
            .l().unwrap();

        let id_val = jni::objects::JValue::Long(account.id as jlong);

        let name_jstr = env.new_string(&account.name).unwrap();
        let name_val = jni::objects::JValue::Object(&name_jstr);

        let issuer_jstr = env.new_string(&account.issuer).unwrap();
        let issuer_val = jni::objects::JValue::Object(&issuer_jstr);

        let note_jstr = env.new_string(&account.note).unwrap();
        let note_val = jni::objects::JValue::Object(&note_jstr);

        let secret_base32 = data_encoding::BASE32_NOPAD.encode(&account.secret);
        let secret_jstr = env.new_string(secret_base32).unwrap();
        let secret_val = jni::objects::JValue::Object(&secret_jstr);

        let code_type_val = jni::objects::JValue::Object(&code_type_obj);
        let algorithm_val = jni::objects::JValue::Object(&algorithm_obj);

        let counter_val = jni::objects::JValue::Long(account.counter as jlong);
        let period_val = jni::objects::JValue::Long(account.period as jlong);
        let digits_val = jni::objects::JValue::Int(account.digits as jint);
        let color_val = jni::objects::JValue::Int(account.color as jint);

        let account_obj = env
            .new_object(
                jni::jni_str!("app/ninesevennine/twofactorauthenticator/AccountWithSecretBase32NoPad"),
                jni::jni_sig!("(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lapp/ninesevennine/twofactorauthenticator/CodeType;Lapp/ninesevennine/twofactorauthenticator/Algorithm;JJII)V"),
                &[
                    id_val,
                    name_val,
                    issuer_val,
                    note_val,
                    secret_val,
                    code_type_val,
                    algorithm_val,
                    counter_val,
                    period_val,
                    digits_val,
                    color_val,
                ],
            ).unwrap();

        Ok(account_obj.into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_getAccountsTruncatedFiltered(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    query: JString,
) -> jobjectArray {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let query: String = query.to_string();

        let client = CLIENT.lock().unwrap();
        let accounts = client.get_accounts_truncated_filtered(&query);

        let account_class = env.find_class(jni::jni_str!(
            "app/ninesevennine/twofactorauthenticator/AccountTruncated"
        )).unwrap();
        let code_type_class = env.find_class(jni::jni_str!(
            "app/ninesevennine/twofactorauthenticator/CodeType"
        )).unwrap();

        let output = env.new_object_array(accounts.len() as jint, &account_class, JObject::null()).unwrap();

        for (i, account) in accounts.iter().enumerate() {
            let code_type_obj = env
                .get_static_field(
                    &code_type_class,
                    match account.code_type {
                        CodeType::HOTP => jni::jni_str!("HOTP"),
                        CodeType::TOTP => jni::jni_str!("TOTP"),
                        CodeType::STEAM => jni::jni_str!("STEAM"),
                    },
                    jni::jni_sig!("Lapp/ninesevennine/twofactorauthenticator/CodeType;"),
                ).unwrap()
                .l().unwrap();

            let id_val = jni::objects::JValue::Long(account.id as jlong);
            let name_jstr = env.new_string(&account.name).unwrap();
            let name_val = jni::objects::JValue::Object(&name_jstr);

            let issuer_jstr = env.new_string(&account.issuer).unwrap();
            let issuer_val = jni::objects::JValue::Object(&issuer_jstr);

            let code_type_val = jni::objects::JValue::Object(&code_type_obj);

            let counter_val = jni::objects::JValue::Long(account.counter as jlong);
            let period_val = jni::objects::JValue::Long(account.period as jlong);
            let digits_val = jni::objects::JValue::Int(account.digits as jint);
            let color_val = jni::objects::JValue::Int(account.color as jint);

            let account_obj = env
                .new_object(
                    &account_class,
                    jni::jni_sig!("(JLjava/lang/String;Ljava/lang/String;Lapp/ninesevennine/twofactorauthenticator/CodeType;JJII)V"),
                    &[
                        id_val,
                        name_val,
                        issuer_val,
                        code_type_val,
                        counter_val,
                        period_val,
                        digits_val,
                        color_val,
                    ],
                ).unwrap();

            output.set_element(env, i, account_obj).unwrap();
        }

        Ok(output.into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_getAccountCode(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    id: jlong,
    counter: jlong,
    show_next: jboolean,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();
        let code = client.get_account_code(id as u64, counter as u64, show_next);

        Ok(env.new_string(code).unwrap().into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_reorderAccount(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    from_index: jint,
    to_index: jint,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        client.reorder_account(from_index as usize, to_index as usize);

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_deleteAccount(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    id: jlong,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();
        client.delete_account(id as u64);

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_incrementAccountCounterAndGet(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    id: jlong,
) -> jlong {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();

        if let Some(ref mut account) = client.edit {
            if account.id == id as u64 {
                account.counter = account.counter.wrapping_add(1);
                return Ok(account.counter as jlong);
            }
        }

        for account in &mut client.accounts {
            if account.id == id as u64 {
                account.counter = account.counter.wrapping_add(1);
                return Ok(account.counter as jlong);
            }
        }

        Ok(0)
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_exportAccountsUsingSeed(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();
        Ok(env
            .new_string(client.export_accounts_using_seed())
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_exportAccountsUsingAutomaticInternalBackups(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();

        Ok(env
            .new_string(client.export_accounts_using_automatic_internal_backups())
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_exportAccounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    pass: JString,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let pass: String = pass.to_string();
        let client = CLIENT.lock().unwrap();

        Ok(env
            .new_string(client.export_accounts(&pass))
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_exportAccountsAegis(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    pass: JString,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let pass: String = pass.to_string();
        let client = CLIENT.lock().unwrap();
        Ok(env
            .new_string(client.export_accounts_aegis(&pass))
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_importAccountsUsingSeed(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    data: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let data: String = data.to_string();
        let mut client = CLIENT.lock().unwrap();

        if client.import_accounts_using_seed(&data) {
            Ok(JNI_TRUE)
        } else {
            Ok(JNI_FALSE)
        }
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_importAccounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    pass: JString,
    data: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let pass: String = pass.to_string();
        let data: String = data.to_string();

        let mut client = CLIENT.lock().unwrap();
        if client.import_accounts(&pass, &data) {
            Ok(JNI_TRUE)
        } else {
            Ok(JNI_FALSE)
        }
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_importAccountsDB001(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    data: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let data: String = data.to_string();
        let mut client = CLIENT.lock().unwrap();
        if client.import_accounts_db_001(&data) {
            Ok(JNI_TRUE)
        } else {
            Ok(JNI_FALSE)
        }
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_importAccountsAegis(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    pass: JString,
    data: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let pass: String = pass.to_string();
        let data: String = data.to_string();

        let mut client = CLIENT.lock().unwrap();
        if client.import_accounts_aegis(&pass, &data) {
            Ok(JNI_TRUE)
        } else {
            Ok(JNI_FALSE)
        }
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_svgToBitmap(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    svg_data: JByteArray,
    width: jint,
    height: jint,
) -> jobject {
    use resvg::tiny_skia;
    use resvg::usvg;

    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        if width <= 0 || height <= 0 {
            return Ok(std::ptr::null_mut());
        }

        let len = match svg_data.len(env) {
            Ok(l) => l as usize,
            Err(_) => return Ok(std::ptr::null_mut()),
        };

        let mut buf_i8 = vec![0i8; len];
        if let Err(_) = svg_data.get_region(env, 0, &mut buf_i8) {
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
