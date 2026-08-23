use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::JClass,
    strings::JNIString,
};
use std::{
    fs::{File, read_to_string, rename},
    io::{ErrorKind, Write},
    path::PathBuf,
};

use crate::{
    CLIENT,
    account::Account,
    features::management::{
        native::{export::export, import::import},
        result::ImportResult,
    },
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1accounts_1load(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let directory = {
            let client = CLIENT.lock().unwrap();

            if client.application_directory.is_empty() {
                env.fatal_error(&JNIString::from("application_directory is empty"))
            }

            PathBuf::from(&client.application_directory)
        };

        let path = directory.join("accounts.2fa");

        let json = match read_to_string(&path) {
            Ok(json) => json,
            Err(e) => {
                if e.kind() == ErrorKind::NotFound {
                    return Ok(());
                }

                env.fatal_error(&JNIString::from(e.to_string()))
            }
        };

        let mut accounts: Vec<Account> = Vec::new();

        match import(&mut accounts, &json) {
            ImportResult::Success => {
                let mut client = CLIENT.lock().unwrap();

                client.accounts = accounts;
            }
            _ => {}
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1accounts_1save(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let (directory, json) = {
            let client = CLIENT.lock().unwrap();

            if client.application_directory.is_empty() {
                env.fatal_error(&JNIString::from("application_directory is empty"))
            }

            (
                PathBuf::from(&client.application_directory),
                export(&client.accounts, &String::new()),
            )
        };

        let path = directory.join("accounts.2fa");
        let tmp = directory.join("accounts.2fa.tmp");

        let result = (|| -> std::io::Result<()> {
            let mut file = File::create(&tmp)?;
            file.write_all(json.as_bytes())?;
            file.sync_all()?;

            let dir = File::open(&directory)?;
            rename(&tmp, &path)?;
            dir.sync_all()
        })();

        if let Err(e) = result {
            env.fatal_error(&JNIString::from(e.to_string()));
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}
