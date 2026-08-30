use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JString},
    strings::JNIString,
};
use std::{
    fs::{File, read_to_string, rename},
    io::{ErrorKind, Write},
    path::PathBuf,
};
use zeroize::{Zeroize, Zeroizing};

use crate::{
    CLIENT,
    account::Account,
    features::management::{
        native::v4::{
            self,
            export::{export, export_encrypted},
        },
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

        let json = Zeroizing::new(match read_to_string(&path) {
            Ok(json) => json,
            Err(e) => {
                if e.kind() == ErrorKind::NotFound {
                    return Ok(());
                }
                env.fatal_error(&JNIString::from(e.to_string()))
            }
        });

        let mut accounts: Vec<Account> = Vec::new();

        match v4::import::import(&mut accounts, &json, &String::new()) {
            ImportResult::Success => {
                let mut client = CLIENT.lock().unwrap();

                client.accounts.zeroize();
                client.accounts = accounts;
            }
            _ => accounts.zeroize(),
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

            let has_key = client.unlock_passphrase_key.iter().any(|&b| b != 0);
            let has_salt = client.unlock_passphrase_salt.iter().any(|&b| b != 0);

            let json = if has_key && has_salt {
                export_encrypted(
                    &client.accounts,
                    &client.unlock_passphrase_salt,
                    &client.unlock_passphrase_key,
                )
            } else {
                let mut salt = Zeroizing::new([0u8; 16]);
                let mut key = Zeroizing::new([0u8; 64]);
                export(&client.accounts, &String::new(), &mut salt, &mut key)
            };

            (
                PathBuf::from(&client.application_directory),
                Zeroizing::new(json),
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

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1accounts_1save_1with_1passphrase(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let passphrase = Zeroizing::new(passphrase.to_string());

        let mut salt = Zeroizing::new([0u8; 16]);
        let mut key = Zeroizing::new([0u8; 64]);

        let (directory, json) = {
            let client = CLIENT.lock().unwrap();

            if client.application_directory.is_empty() {
                env.fatal_error(&JNIString::from("application_directory is empty"))
            }

            (
                PathBuf::from(&client.application_directory),
                Zeroizing::new(export(&client.accounts, &passphrase, &mut salt, &mut key)),
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

        let mut client = CLIENT.lock().unwrap();

        client.unlock_passphrase_salt.zeroize();
        client.unlock_passphrase_key.zeroize();

        if !passphrase.is_empty() {
            client.unlock_passphrase_salt = *salt;
            client.unlock_passphrase_key = *key;
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}
