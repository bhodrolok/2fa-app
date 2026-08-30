use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JString},
    sys::jboolean,
};

use crate::CLIENT;

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_client_1application_1directory_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    application_directory: JString,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let application_directory = application_directory.to_string();

        let mut client = CLIENT.lock().unwrap();

        client.application_directory = application_directory;

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_client_1has_1salt_1and_1key(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let client = CLIENT.lock().unwrap();

        let has_key = client.unlock_passphrase_key.iter().any(|&b| b != 0);
        let has_salt = client.unlock_passphrase_salt.iter().any(|&b| b != 0);

        Ok(has_key && has_salt)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}
