use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JByteArray, JClass, JString},
    sys::{jbyteArray, jint},
};
use zeroize::{Zeroize, Zeroizing};

use crate::{
    CLIENT,
    account::Account,
    features::management::{
        native::{
            v3,
            v4::{
                self,
                export::export,
                schema::{KEY_LEN, SALT_LEN},
            },
        },
        result::ImportResult,
    },
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_1native_1export_1accounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
) -> jbyteArray {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let passphrase = Zeroizing::new(passphrase.to_string());

        let mut salt = Zeroizing::new([0u8; SALT_LEN]);
        let mut key = Zeroizing::new([0u8; KEY_LEN]);

        let client = CLIENT.lock().unwrap();

        let json = export(&client.accounts, &passphrase, &mut salt, &mut key);

        Ok(env
            .byte_array_from_slice(json.as_bytes())
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_1native_1import_1into_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    bytes: JByteArray,
    passphrase: JString,
) -> jint {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let mut accounts: Vec<Account> = Vec::new();

        let bytes = Zeroizing::new(env.convert_byte_array(&bytes).unwrap());
        let passphrase = Zeroizing::new(passphrase.to_string());

        let json = Zeroizing::new(match std::str::from_utf8(&bytes) {
            Ok(s) => s.to_string(),
            Err(_) => return Ok(ImportResult::InvalidFormat as jint),
        });

        let mut salt = Zeroizing::new([0u8; SALT_LEN]);
        let mut key = Zeroizing::new([0u8; KEY_LEN]);

        match v4::import::import(&json, &passphrase, &mut salt, &mut key, &mut accounts) {
            ImportResult::Success => {
                let count = accounts.len() as jint;

                let mut client = CLIENT.lock().unwrap();

                client.hostages.zeroize();
                client.hostages = accounts;

                Ok(count)
            }
            ImportResult::IncorrectPassphrase => {
                accounts.zeroize();

                Ok(ImportResult::IncorrectPassphrase as jint)
            }
            _ => match v3::import::import(&json, &passphrase, &mut salt, &mut key, &mut accounts) {
                ImportResult::Success => {
                    let count = accounts.len() as jint;

                    let mut client = CLIENT.lock().unwrap();

                    client.hostages.zeroize();
                    client.hostages = accounts;

                    Ok(count)
                }
                err => {
                    accounts.zeroize();

                    Ok(err as jint)
                }
            },
        }
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}
