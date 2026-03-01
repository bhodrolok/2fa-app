use crate::{
    CLIENT,
    client::SECRET_SIZE,
    features::codegen::{Algorithm, CodeType},
};
use jni::{
    JNIEnv,
    objects::{JByteArray, JClass, JIntArray, JObject, JString},
    sys::{JNI_FALSE, JNI_TRUE, jboolean, jbyteArray, jint, jlong, jobject, jobjectArray, jstring},
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_generateClientSeedWithEntropy(
    env: JNIEnv,
    _class: JClass,
    entropy_array: JIntArray,
) {
    use sha3::Digest;
    use sha3::Sha3_512;

    let len = env.get_array_length(&entropy_array).unwrap() as usize;

    let mut entropy_vec = vec![0i32; len];
    env.get_int_array_region(entropy_array, 0, &mut entropy_vec)
        .unwrap();

    let entropy_bytes: Vec<u8> = entropy_vec
        .iter()
        .flat_map(|val| val.to_le_bytes())
        .collect();

    // mix entropy
    let mut hasher = Sha3_512::new();
    hasher.update(&entropy_bytes);

    // with system randomness
    let mut system_random = [0u8; 64];
    getrandom::fill(&mut system_random).unwrap();
    hasher.update(&system_random);

    let hash = hasher.finalize();

    let mut client = CLIENT.lock().unwrap();
    client.seed = hash.into();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setAutomaticInternalBackupsPass(
    mut env: JNIEnv,
    _class: JClass,
    pass: JString,
) {
    let pass: String = env.get_string(&pass).unwrap().into();

    let mut client = CLIENT.lock().unwrap();
    client.set_automatic_internal_backups_pass(&pass);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_getSecret(
    env: JNIEnv,
    _class: JClass,
) -> jbyteArray {
    let client = CLIENT.lock().unwrap();
    let secret = client.get_secret();

    let output = env.new_byte_array(secret.len() as i32).unwrap();
    let secret_i8: Vec<i8> = secret.iter().map(|&b| b as i8).collect();
    env.set_byte_array_region(&output, 0, &secret_i8).unwrap();

    output.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setSecret(
    env: JNIEnv,
    _class: JClass,
    secret_array: JByteArray,
) {
    let len = env.get_array_length(&secret_array).unwrap() as usize;
    let mut secret_vec = vec![0i8; len];
    env.get_byte_array_region(&secret_array, 0, &mut secret_vec)
        .unwrap();

    let secret_bytes: Vec<u8> = secret_vec.iter().map(|&b| b as u8).collect();
    let mut secret = [0u8; SECRET_SIZE];
    secret.copy_from_slice(&secret_bytes);

    let mut client = CLIENT.lock().unwrap();
    client.set_secret(&secret);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_isSecretEmpty(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    let client = CLIENT.lock().unwrap();
    let secret = client.get_secret();

    if secret.iter().all(|&b| b == 0) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountEmpty(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut client = CLIENT.lock().unwrap();
    client.set_edit_account_empty();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountNone(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut client = CLIENT.lock().unwrap();
    client.edit = None
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountFromOtpAuth(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jboolean {
    let url: String = env.get_string(&url).unwrap().into();

    let mut client = crate::CLIENT.lock().unwrap();

    if client.set_edit_account_from_otpauth(&url) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_copyEditAccountIntoAccounts(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut client = CLIENT.lock().unwrap();
    client.copy_edit_account_into_accounts();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_copyAccountIntoEditAccount(
    _env: JNIEnv,
    _class: JClass,
    id: jlong,
) {
    let mut client = CLIENT.lock().unwrap();
    client.copy_account_into_edit_account(id as u64);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountName(
    mut env: JNIEnv,
    _class: JClass,
    name: JString,
) {
    let name: String = env.get_string(&name).unwrap().into();

    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        account.name = name;
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountIssuer(
    mut env: JNIEnv,
    _class: JClass,
    issuer: JString,
) {
    let issuer: String = env.get_string(&issuer).unwrap().into();

    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        account.issuer = issuer;
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountNote(
    mut env: JNIEnv,
    _class: JClass,
    note: JString,
) {
    let note: String = env.get_string(&note).unwrap().into();

    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        account.note = note;
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountSecretBase32NoPad(
    mut env: JNIEnv,
    _class: JClass,
    secret_base32_nopad: JString,
) -> jboolean {
    let secret_base32_nopad: String = env.get_string(&secret_base32_nopad).unwrap().into();

    let secret_vec = match data_encoding::BASE32_NOPAD.decode(secret_base32_nopad.as_bytes()) {
        Ok(v) => v,
        Err(_) => return JNI_FALSE,
    };

    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        account.secret = secret_vec;
    }

    JNI_TRUE
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountCodeType(
    mut env: JNIEnv,
    _class: JClass,
    code_type_enum: JObject,
) {
    let value = env
        .call_method(&code_type_enum, "ordinal", "()I", &[])
        .unwrap()
        .i()
        .unwrap();

    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        account.code_type = match value {
            0 => CodeType::HOTP,
            1 => CodeType::TOTP,
            2 => CodeType::STEAM,
            _ => return,
        };
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountAlgorithm(
    mut env: JNIEnv,
    _class: JClass,
    algorithm_enum: JObject,
) {
    let value = env
        .call_method(&algorithm_enum, "ordinal", "()I", &[])
        .unwrap()
        .i()
        .unwrap();

    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        account.algorithm = match value {
            0 => Algorithm::SHA1,
            1 => Algorithm::SHA256,
            2 => Algorithm::SHA512,
            _ => return,
        };
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountCounter(
    _env: JNIEnv,
    _class: JClass,
    counter: jlong,
) {
    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        account.counter = counter as u64;
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountPeriod(
    _env: JNIEnv,
    _class: JClass,
    period: jlong,
) {
    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        account.period = period as u64;
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountDigits(
    _env: JNIEnv,
    _class: JClass,
    digits: jint,
) {
    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        account.digits = digits as u32;
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_setEditAccountColor(
    _env: JNIEnv,
    _class: JClass,
    color: jint,
) {
    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        account.color = color as u32;
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_isEditAccountSome(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    let client = crate::CLIENT.lock().unwrap();

    if client.edit.is_some() {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_getEditAccountWithSecretBase32NoPad(
    mut env: JNIEnv,
    _class: JClass,
) -> jobject {
    let client = CLIENT.lock().unwrap();

    let Some(ref account) = client.edit else {
        return std::ptr::null_mut();
    };

    let code_type_class = env
        .find_class("app/ninesevennine/twofactorauthenticator/CodeType")
        .unwrap();
    let code_type_obj = env
        .get_static_field(
            code_type_class,
            match account.code_type {
                CodeType::HOTP => "HOTP",
                CodeType::TOTP => "TOTP",
                CodeType::STEAM => "STEAM",
            },
            "Lapp/ninesevennine/twofactorauthenticator/CodeType;",
        )
        .unwrap()
        .l()
        .unwrap();

    let algorithm_class = env
        .find_class("app/ninesevennine/twofactorauthenticator/Algorithm")
        .unwrap();
    let algorithm_obj = env
        .get_static_field(
            algorithm_class,
            match account.algorithm {
                Algorithm::SHA1 => "SHA1",
                Algorithm::SHA256 => "SHA256",
                Algorithm::SHA512 => "SHA512",
            },
            "Lapp/ninesevennine/twofactorauthenticator/Algorithm;",
        )
        .unwrap()
        .l()
        .unwrap();

    let account_obj = env
        .new_object(
            "app/ninesevennine/twofactorauthenticator/AccountWithSecretBase32NoPad",
            "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lapp/ninesevennine/twofactorauthenticator/CodeType;Lapp/ninesevennine/twofactorauthenticator/Algorithm;JJII)V",
            &[
                jni::objects::JValue::Long(account.id as jlong),
                jni::objects::JValue::Object(&env.new_string(&account.name).unwrap()),
                jni::objects::JValue::Object(&env.new_string(&account.issuer).unwrap()),
                jni::objects::JValue::Object(&env.new_string(&account.note).unwrap()),
                jni::objects::JValue::Object(&env.new_string(data_encoding::BASE32_NOPAD.encode(&account.secret)).unwrap()),
                jni::objects::JValue::Object(&code_type_obj),
                jni::objects::JValue::Object(&algorithm_obj),
                jni::objects::JValue::Long(account.counter as jlong),
                jni::objects::JValue::Long(account.period as jlong),
                jni::objects::JValue::Int(account.digits as jint),
                jni::objects::JValue::Int(account.color as jint),
            ],
        )
        .unwrap();

    account_obj.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_getAccountsTruncatedFiltered(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
) -> jobjectArray {
    let query: String = env.get_string(&query).unwrap().into();

    let client = CLIENT.lock().unwrap();
    let accounts = client.get_accounts_truncated_filtered(&query);

    let account_class = env
        .find_class("app/ninesevennine/twofactorauthenticator/AccountTruncated")
        .unwrap();
    let code_type_class = env
        .find_class("app/ninesevennine/twofactorauthenticator/CodeType")
        .unwrap();

    let output = env
        .new_object_array(accounts.len() as jint, &account_class, JObject::null())
        .unwrap();

    for (i, account) in accounts.iter().enumerate() {
        let code_type_obj = env
            .get_static_field(
                &code_type_class,
                match account.code_type {
                    CodeType::HOTP => "HOTP",
                    CodeType::TOTP => "TOTP",
                    CodeType::STEAM => "STEAM",
                },
                "Lapp/ninesevennine/twofactorauthenticator/CodeType;",
            )
            .unwrap()
            .l()
            .unwrap();

        let account_obj = env
            .new_object(
                &account_class,
                "(JLjava/lang/String;Ljava/lang/String;Lapp/ninesevennine/twofactorauthenticator/CodeType;JJII)V",
                &[
                    jni::objects::JValue::Long(account.id as jlong),
                    jni::objects::JValue::Object(&env.new_string(&account.name).unwrap()),
                    jni::objects::JValue::Object(&env.new_string(&account.issuer).unwrap()),
                    jni::objects::JValue::Object(&code_type_obj),
                    jni::objects::JValue::Long(account.counter as jlong),
                    jni::objects::JValue::Long(account.period as jlong),
                    jni::objects::JValue::Int(account.digits as jint),
                    jni::objects::JValue::Int(account.color as jint),
                ],
            )
            .unwrap();

        env.set_object_array_element(&output, i as jint, account_obj)
            .unwrap();
    }

    output.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_getAccountCode(
    env: JNIEnv,
    _class: JClass,
    id: jlong,
    counter: jlong,
    show_next: jboolean,
) -> jstring {
    let client = CLIENT.lock().unwrap();

    let code = client.get_account_code(id as u64, counter as u64, show_next != 0);

    env.new_string(code).unwrap().into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_reorderAccount(
    _env: JNIEnv,
    _class: JClass,
    from_index: jint,
    to_index: jint,
) {
    let mut client = CLIENT.lock().unwrap();
    client.reorder_account(from_index as usize, to_index as usize);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_deleteAccount(
    _env: JNIEnv,
    _class: JClass,
    id: jlong,
) {
    let mut client = CLIENT.lock().unwrap();
    client.delete_account(id as u64);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_incrementAccountCounterAndGet(
    _env: JNIEnv,
    _class: JClass,
    id: jlong,
) -> jlong {
    let mut client = CLIENT.lock().unwrap();

    if let Some(ref mut account) = client.edit {
        if account.id == id as u64 {
            account.counter = account.counter.wrapping_add(1);
            return account.counter as jlong;
        }
    }

    for account in &mut client.accounts {
        if account.id == id as u64 {
            account.counter = account.counter.wrapping_add(1);
            return account.counter as jlong;
        }
    }

    0
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_exportAccountsUsingSeed(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let client = CLIENT.lock().unwrap();

    let data = client.export_accounts_using_seed();

    env.new_string(data).unwrap().into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_exportAccountsUsingAutomaticInternalBackups(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let client = CLIENT.lock().unwrap();

    let data = client.export_accounts_using_automatic_internal_backups();

    env.new_string(data).unwrap().into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_exportAccounts(
    mut env: JNIEnv,
    _class: JClass,
    pass: JString,
) -> jstring {
    let pass: String = env.get_string(&pass).unwrap().into();

    let client = CLIENT.lock().unwrap();
    let data = client.export_accounts(&pass);

    env.new_string(data).unwrap().into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_exportAccountsAegis(
    mut env: JNIEnv,
    _class: JClass,
    pass: JString,
) -> jstring {
    let pass: String = env.get_string(&pass).unwrap().into();

    let client = CLIENT.lock().unwrap();
    let data = client.export_accounts_aegis(&pass);

    env.new_string(data).unwrap().into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_importAccountsUsingSeed(
    mut env: JNIEnv,
    _class: JClass,
    data: JString,
) -> jboolean {
    let data: String = env.get_string(&data).unwrap().into();

    let mut client = CLIENT.lock().unwrap();

    if client.import_accounts_using_seed(&data) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_importAccounts(
    mut env: JNIEnv,
    _class: JClass,
    pass: JString,
    data: JString,
) -> jboolean {
    let pass: String = env.get_string(&pass).unwrap().into();
    let data: String = env.get_string(&data).unwrap().into();

    let mut client = CLIENT.lock().unwrap();

    if client.import_accounts(&pass, &data) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_importAccountsDB001(
    mut env: JNIEnv,
    _class: JClass,
    data: JString,
) -> jboolean {
    let data: String = env.get_string(&data).unwrap().into();

    let mut client = CLIENT.lock().unwrap();

    if client.import_accounts_db_001(&data) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_importAccountsAegis(
    mut env: JNIEnv,
    _class: JClass,
    pass: JString,
    data: JString,
) -> jboolean {
    let pass: String = env.get_string(&pass).unwrap().into();
    let data: String = env.get_string(&data).unwrap().into();

    let mut client = CLIENT.lock().unwrap();

    if client.import_accounts_aegis(&pass, &data) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}
