use jni::{EnvUnowned, objects::JClass, strings::JNIString};
use std::{fs::File, io::Write, path::PathBuf};

use crate::CLIENT;

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1save_1test(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        let path = {
            let client = CLIENT.lock().unwrap();
            PathBuf::from(&client.application_directory).join("test.txt")
        };

        let result = (|| -> std::io::Result<()> {
            let mut file = File::create(&path)?;
            file.write_all(b"Hello from Rust!\n")?;
            file.sync_all()
        })();

        if let Err(e) = result {
            env.fatal_error(&JNIString::from(e.to_string()));
        }

        Ok(())
    });
    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1copy_1accounts_1into_1hostages(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, jni::errors::Error> {
        let mut client = CLIENT.lock().unwrap();

        client.hostages = client.accounts.clone();

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
