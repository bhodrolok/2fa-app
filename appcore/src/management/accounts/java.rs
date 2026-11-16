use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JString},
    strings::JNIString,
    sys::jint,
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
        native::{
            v3,
            v4::{
                self,
                export::{export, export_encrypted},
                schema::{KEY_LEN, SALT_LEN},
            },
        },
        result::ImportResult,
    },
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1accounts_1load__(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jint {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let (directory, key) = {
            let client = CLIENT.lock().unwrap();

            if client.application_directory.is_empty() {
                env.fatal_error(&JNIString::from("application_directory is empty"))
            }

            (
                PathBuf::from(&client.application_directory),
                Zeroizing::new(client.key),
            )
        };

        let path = directory.join("accounts.2fa");

        let json = Zeroizing::new(match read_to_string(&path) {
            Ok(json) => json,
            Err(e) => {
                if e.kind() == ErrorKind::NotFound {
                    CLIENT.lock().unwrap().unlocked = true;

                    return Ok(ImportResult::Success as jint);
                }

                env.fatal_error(&JNIString::from(e.to_string()))
            }
        });

        let mut accounts: Vec<Account> = Vec::new();

        let mut salt = Zeroizing::new([0u8; SALT_LEN]);
        let has_key = key.iter().any(|&b| b != 0);

        let result = if has_key {
            match v4::import::import_encrypted(&json, &key, &mut salt, &mut accounts) {
                ImportResult::Success => ImportResult::Success,
                ImportResult::IncorrectPassphrase => ImportResult::IncorrectPassphrase,
                _ => v3::import::import_encrypted(&json, &key, &mut salt, &mut accounts),
            }
        } else {
            let mut unused_key = Zeroizing::new([0u8; KEY_LEN]);

            v4::import::import(
                &json,
                &String::new(),
                &mut salt,
                &mut unused_key,
                &mut accounts,
            )
        };

        match result {
            ImportResult::Success => {
                let count = accounts.len() as jint;

                let mut client = CLIENT.lock().unwrap();

                client.unlocked = true;

                client.accounts.zeroize();
                client.accounts = accounts;
                client.salt = *salt;

                Ok(count)
            }
            err => {
                accounts.zeroize();

                Ok(err as jint)
            }
        }
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1accounts_1load__Ljava_lang_String_2(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
) -> jint {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let passphrase = Zeroizing::new(passphrase.to_string());

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
                    CLIENT.lock().unwrap().unlocked = true;

                    return Ok(ImportResult::Success as jint);
                }

                env.fatal_error(&JNIString::from(e.to_string()))
            }
        });

        let mut accounts: Vec<Account> = Vec::new();

        let mut salt = Zeroizing::new([0u8; SALT_LEN]);
        let mut key = Zeroizing::new([0u8; KEY_LEN]);

        let result =
            match v4::import::import(&json, &passphrase, &mut salt, &mut key, &mut accounts) {
                ImportResult::Success => ImportResult::Success,
                ImportResult::IncorrectPassphrase => ImportResult::IncorrectPassphrase,
                _ => v3::import::import(&json, &passphrase, &mut salt, &mut key, &mut accounts),
            };

        match result {
            ImportResult::Success => {
                let count = accounts.len() as jint;

                let mut client = CLIENT.lock().unwrap();

                client.unlocked = true;

                client.accounts.zeroize();
                client.accounts = accounts;

                client.salt.zeroize();
                client.key.zeroize();

                if !passphrase.is_empty() {
                    client.salt = *salt;
                    client.key = *key;
                }

                Ok(count)
            }
            err => {
                accounts.zeroize();

                Ok(err as jint)
            }
        }
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1accounts_1save__(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let (directory, json) = {
            let client = CLIENT.lock().unwrap();

            if client.application_directory.is_empty() {
                env.fatal_error(&JNIString::from("application_directory is empty"))
            }

            if !client.unlocked {
                return Ok(());
            }

            let has_key = client.key.iter().any(|&b| b != 0);
            let has_salt = client.salt.iter().any(|&b| b != 0);

            let json = if has_key && has_salt {
                export_encrypted(&client.accounts, &client.salt, &client.key)
            } else {
                let mut salt = Zeroizing::new([0u8; SALT_LEN]);
                let mut key = Zeroizing::new([0u8; KEY_LEN]);
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
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1accounts_1save__Ljava_lang_String_2(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
) {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let passphrase = Zeroizing::new(passphrase.to_string());

        let mut salt = Zeroizing::new([0u8; SALT_LEN]);
        let mut key = Zeroizing::new([0u8; KEY_LEN]);

        let (directory, json) = {
            let mut client = CLIENT.lock().unwrap();

            if client.application_directory.is_empty() {
                env.fatal_error(&JNIString::from("application_directory is empty"))
            }

            if !client.unlocked {
                return Ok(());
            }

            let json = Zeroizing::new(export(&client.accounts, &passphrase, &mut salt, &mut key));

            client.salt.zeroize();
            client.key.zeroize();

            if !passphrase.is_empty() {
                client.salt = *salt;
                client.key = *key;
            }

            (PathBuf::from(&client.application_directory), json)
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
