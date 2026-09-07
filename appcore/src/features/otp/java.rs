use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::JClass,
    sys::{jboolean, jlong, jstring},
};

use crate::{
    CLIENT,
    features::otp::{hotp, steam, r#type::CodeType},
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1otp_1generate(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    id: jlong,
    counter: jlong,
    next: jboolean,
    revealed: jboolean,
) -> jstring {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let id = id as u64;

        let client = CLIENT.lock().unwrap();

        let account = client
            .edit
            .as_ref()
            .filter(|account| account.id == id)
            .or_else(|| client.accounts.iter().find(|account| account.id == id));

        let Some(account) = account else {
            return Ok(env.new_string("").unwrap().into_raw());
        };

        if account.secret.is_empty() {
            return Ok(env
                .new_string("?".repeat(account.digits as usize))
                .unwrap()
                .into_raw());
        }

        if !revealed {
            return Ok(env
                .new_string("•".repeat(account.digits as usize))
                .unwrap()
                .into_raw());
        }

        let code = match account.code_type {
            CodeType::TOTP => hotp::generate(
                &account.secret,
                account.algorithm,
                account.digits,
                counter as u64 + u64::from(next),
            ),
            CodeType::HOTP => hotp::generate(
                &account.secret,
                account.algorithm,
                account.digits,
                account.counter,
            ),
            CodeType::STEAM => steam::generate(
                &account.secret,
                (counter as u64 + u64::from(next)) * account.period,
            ),
        };

        Ok(env.new_string(code).unwrap().into_raw())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1otp_1counter_1increment(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    id: jlong,
) -> jlong {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let id = id as u64;

        let mut client = CLIENT.lock().unwrap();

        let edit_match = client.edit.as_ref().is_some_and(|account| account.id == id);

        let account = if edit_match {
            client.edit.as_mut()
        } else {
            client.accounts.iter_mut().find(|account| account.id == id)
        };

        let Some(account) = account else {
            return Ok(0);
        };

        account.counter = account.counter.wrapping_add(1);

        Ok(account.counter as jlong)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}
