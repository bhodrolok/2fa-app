use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JString},
    sys::jboolean,
};
use zeroize::Zeroize;

use crate::{CLIENT, features::otpauth::deserialize::deserialize};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1otpauth_1import_1into_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    uri: JString,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let uri = uri.to_string();

        if let Some(account) = deserialize(&uri) {
            let mut client = CLIENT.lock().unwrap();
            client.hostages.zeroize();
            client.hostages.push(account);

            return Ok(true);
        }

        Ok(false)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}
