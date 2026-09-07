use jni::{
    EnvUnowned, JValue,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JObject, JString},
    sys::{jint, jlong, jobjectArray},
};

use crate::{CLIENT, features::ngram};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1main_1search(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    query: JString,
) -> jobjectArray {
    let outcome = unowned_env.with_env(|env| -> Result<_, Error> {
        let query: String = query.to_string();

        let accounts = {
            let client = CLIENT.lock().unwrap();
            ngram::search(&client.accounts, &query)
        };

        let account_class = env.find_class(jni::jni_str!(
            "app/ninesevennine/twofactorauthenticator/AccountShort"
        ))?;

        let output = env
            .new_object_array(accounts.len() as jint, &account_class, JObject::null())
            .unwrap();

        for (i, account) in accounts.iter().enumerate() {
            let name_jstr = env.new_string(&account.name).unwrap();
            let issuer_jstr = env.new_string(&account.issuer).unwrap();

            let account_obj = env
                .new_object(
                    &account_class,
                    jni::jni_sig!("(JLjava/lang/String;Ljava/lang/String;IJJI)V"),
                    &[
                        JValue::Long(account.id as jlong),
                        JValue::Object(&name_jstr),
                        JValue::Object(&issuer_jstr),
                        JValue::Int(account.code_type as jint),
                        JValue::Long(account.counter as jlong),
                        JValue::Long(account.period as jlong),
                        JValue::Int(account.color as jint),
                    ],
                )
                .unwrap();

            output.set_element(env, i, account_obj).unwrap();
        }

        Ok(output.into_raw())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1main_1reorder(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    from: jlong,
    to: jlong,
) {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let mut client = CLIENT.lock().unwrap();
        let len = client.accounts.len();

        let from = from as usize;
        let to = to as usize;

        if from < len && to < len {
            let account = client.accounts.remove(from);
            client.accounts.insert(to, account);
        }

        Ok(())
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>();
}
