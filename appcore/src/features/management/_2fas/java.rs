use jni::{
    EnvUnowned,
    objects::{JClass, JString},
    sys::{jint, jstring},
};

use crate::{
    CLIENT,
    features::{
        management::{
            _2fas::{export::export, import::import},
            result::ImportResult,
        },
        old_otpauth::Account,
    },
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_12fas_1export_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let passphrase: String = passphrase.to_string();

        let client = CLIENT.lock().unwrap();

        Ok(env
            .new_string(export(&client.hostages, &passphrase))
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_12fas_1import_1into_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    json: JString,
    passphrase: JString,
) -> jint {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut accounts: Vec<Account> = Vec::new();

        let json: String = json.to_string();
        let passphrase: String = passphrase.to_string();

        match import(&mut accounts, &json, &passphrase) {
            ImportResult::Success => {
                let count = accounts.len() as jint;

                let mut client = CLIENT.lock().unwrap();

                client.hostages = accounts;

                Ok(count)
            }
            err => Ok(err as jint),
        }
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
