use jni::{EnvUnowned, objects::JClass};
use zeroize::Zeroize;

use crate::CLIENT;

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1zeroize_1hostages(
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

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1merge_1hostages_1into_1accounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();

        let hostages = client.hostages.clone();
        let mut accounts = client.accounts.clone();
        let mut merged = Vec::new();

        for hostage in hostages {
            if let Some(pos) = accounts.iter().position(|a| a.id == hostage.id) {
                merged.extend(accounts.drain(..pos));
                accounts.remove(0);
            }

            merged.push(hostage);
        }

        merged.extend(accounts);
        client.accounts = merged;

        Ok(())
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}
