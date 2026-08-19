use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::JClass,
};

use crate::CLIENT;

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1edit_1none(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        client.edit = None;

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1import_1edit_1into_1accounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        let Some(account) = client.edit.take() else {
            return Ok(());
        };

        match client.accounts.iter().position(|a| a.id == account.id) {
            Some(i) => client.accounts[i] = account,
            None => client.accounts.push(account),
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}
