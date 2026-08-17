use jni::{
    EnvUnowned,
    objects::{JClass, JString},
};
use zeroize::Zeroize;

use crate::CLIENT;

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_client_1application_1directory_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    application_directory: JString,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let application_directory = application_directory.to_string();

        let mut client = CLIENT.lock().unwrap();

        client.application_directory = application_directory;

        Ok(())
    });
    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_client_1zeroize_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();

        client.hostages.zeroize();

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}
