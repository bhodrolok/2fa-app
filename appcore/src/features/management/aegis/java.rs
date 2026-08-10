use jni::{
    EnvUnowned,
    objects::{JClass, JString},
    sys::jint,
};

use crate::{
    CLIENT,
    features::{
        management::{aegis::import, result::ImportResult},
        old_otpauth::Account,
    },
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_1aegis_1import_1into_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    json: JString,
    passphrase: JString,
) -> jint {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut accounts: Vec<Account> = Vec::new();

        let json: String = json.to_string();
        let passphrase: String = passphrase.to_string();

        match import::v3(&mut accounts, &json, &passphrase) {
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
