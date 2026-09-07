use jni::{
    EnvUnowned,
    errors::{Error, ThrowRuntimeExAndDefault},
    objects::{JClass, JString},
};
use zeroize::Zeroizing;
use zxcvbn::zxcvbn;

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_ninesevennine_twofactorauthenticator_RustInterface_features_1zxcvbn_1normalized(
    mut unowned_env: EnvUnowned,
    _class: JClass,
    passphrase: JString,
) -> f32 {
    let outcome = unowned_env.with_env(|_env| -> Result<_, Error> {
        let passphrase = Zeroizing::new(passphrase.to_string());

        let estimate = zxcvbn(&passphrase, &[]);

        let min_log = 3.0;
        let max_log = 18.0;

        Ok(((estimate.guesses_log10() - min_log) / (max_log - min_log)).clamp(0.0, 1.0) as f32)
    });

    outcome.resolve::<ThrowRuntimeExAndDefault>()
}
