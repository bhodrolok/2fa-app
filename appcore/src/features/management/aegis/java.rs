use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JByteArray, JClass, JString},
    sys::{jint, jstring},
};

use crate::{
    CLIENT,
    account::Account,
    features::management::{
        aegis::{export::export, import::import},
        result::ImportResult,
    },
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_1aegis_1export_1accounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let passphrase: String = passphrase.to_string();

        let client = CLIENT.lock().unwrap();

        Ok(env
            .new_string(export(&client.accounts, &passphrase))
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_1aegis_1import_1into_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    bytes: JByteArray,
    passphrase: JString,
) -> jint {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let mut accounts: Vec<Account> = Vec::new();

        let bytes = env.convert_byte_array(&bytes).unwrap();
        let passphrase = passphrase.to_string();

        let json = match String::from_utf8(bytes) {
            Ok(s) => s,
            Err(_) => return Ok(ImportResult::InvalidFormat as jint),
        };

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

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}
