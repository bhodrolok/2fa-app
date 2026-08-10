use jni::{
    EnvUnowned,
    objects::{JClass, JString},
    sys::{jint, jstring},
};

use crate::{
    CLIENT,
    features::{
        management::{
            plain::{export, import},
            result::ImportResult,
        },
        old_otpauth::Account,
    },
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_1plain_1export_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();

        Ok(env
            .new_string(export::v1(&client.hostages))
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_1plain_1import_1into_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    txt: JString,
) -> jint {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut accounts: Vec<Account> = Vec::new();

        let txt: String = txt.to_string();

        match import::v1(&mut accounts, &txt) {
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
