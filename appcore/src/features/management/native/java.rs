use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JString},
    sys::jbyteArray,
};

use crate::{CLIENT, features::management::native::export::export};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1management_1native_1export_1accounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
) -> jbyteArray {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let passphrase: String = passphrase.to_string();

        let client = CLIENT.lock().unwrap();

        let json = export(&client.accounts, &passphrase);

        Ok(env
            .byte_array_from_slice(json.as_bytes())
            .unwrap()
            .into_raw())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}
