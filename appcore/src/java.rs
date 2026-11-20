use std::ffi::c_void;
use zeroize::Zeroize;

use crate::CLIENT;

#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnUnload(_vm: *mut c_void, _reserved: *mut c_void) {
    if let Ok(mut client) = CLIENT.lock() {
        client.loaded = false;
        client.unlocked = false;
        client.application_directory.zeroize();
        client.salt.zeroize();
        client.key.zeroize();
        if let Some(mut account) = client.edit.take() {
            account.zeroize();
        }
        client.accounts.zeroize();
        client.hostages.zeroize();

        // Icons
        client.slot = 0;
        client.name.clear();
        client.timestamp = 0;
        client.map.clear();
    }
}
