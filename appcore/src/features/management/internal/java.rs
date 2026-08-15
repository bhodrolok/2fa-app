use jni::{EnvUnowned, objects::JClass, sys::jstring};

use crate::{CLIENT, features::management::internal::export::export};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_1internal_1export_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let client = CLIENT.lock().unwrap();

        Ok(env.new_string(export(&client.hostages)).unwrap().into_raw())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
