use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JByteArray, JClass, JString},
    sys::{jboolean, jbyteArray},
};
use zeroize::{Zeroize, Zeroizing};

use crate::{CLIENT, features::management::native::v4::schema::KEY_LEN};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_client_1loaded_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    value: jboolean,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        client.loaded = value;

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_client_1loaded_1get(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let client = CLIENT.lock().unwrap();

        Ok(client.loaded)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

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

        let has_key = client.key.iter().any(|&b| b != 0);
        let has_salt = client.salt.iter().any(|&b| b != 0);

        Ok(has_key && has_salt)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_client_1key_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    key: JByteArray,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let mut buffer = Zeroizing::new([0i8; KEY_LEN]);
        key.get_region(env, 0, buffer.as_mut_slice()).unwrap();

        let mut client = CLIENT.lock().unwrap();

        client.key = buffer.map(|b| b as u8);

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_client_1key_1get(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jbyteArray {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let client = CLIENT.lock().unwrap();

        let buffer = Zeroizing::new(client.key.map(|b| b as i8));
        let key = env.new_byte_array(KEY_LEN).unwrap();
        key.set_region(env, 0, buffer.as_slice()).unwrap();

        Ok(key.into_raw())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_client_1zeroize_1sensitive(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        client.salt.zeroize();
        client.key.zeroize();
        if let Some(mut account) = client.edit.take() {
            account.zeroize();
        }
        client.accounts.zeroize();
        client.hostages.zeroize();

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}
