use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JString},
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
