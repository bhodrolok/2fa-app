package app.ninesevennine.twofactorauthenticator.features

import android.app.Activity
import android.view.WindowManager

fun Activity.setScreenSecure(isSecure: Boolean) {
    if (isSecure) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}