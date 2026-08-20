use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::JClass,
    sys::jlong,
};
use zeroize::Zeroize;

use crate::{_client::Client, CLIENT};

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

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1import_1account_1into_1edit(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    id: jlong,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let id = id as u64;

        let mut client = CLIENT.lock().unwrap();

        client.edit = client
            .accounts
            .iter()
            .find(|account| account.id == id)
            .cloned();

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1import_1hostages_1into_1accounts(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        let Client {
            accounts, hostages, ..
        } = &mut *client;

        for hostage in hostages.drain(..) {
            match accounts.iter_mut().find(|a| a.id == hostage.id) {
                Some(existing) => *existing = hostage,
                None => accounts.push(hostage),
            }
        }

        hostages.zeroize();

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}
