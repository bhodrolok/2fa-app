use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::JClass,
    sys::jboolean,
};
use zeroize::Zeroize;

use crate::{
    CLIENT,
    account::Account,
    features::otp::{algorithm::Algorithm, r#type::CodeType},
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1edit_1is_1none(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) -> jboolean {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let client = CLIENT.lock().unwrap();

        Ok(client.edit.is_none())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1edit_1none_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        if let Some(mut account) = client.edit.take() {
            account.zeroize();
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_management_1edit_1default_1set(
    mut unowned_env: EnvUnowned,
    _class: JClass,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();

        if let Some(account) = &mut client.edit {
            account.zeroize();
        }

        client.edit = Some(Account {
            id: getrandom::u64().unwrap(),
            name: String::new(),
            issuer: String::new(),
            note: String::new(),
            secret: Vec::new(),
            code_type: CodeType::TOTP,
            algorithm: Algorithm::SHA1,
            counter: 0,
            period: 30,
            digits: 6,
            color: 0xFF808080,
        });

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}
