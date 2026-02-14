package app.ninesevennine.twofactorauthenticator.features

import android.app.Activity
import android.view.WindowManager

fun setScreenSecure(activity: Activity, secure: Boolean) {
    if (secure) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}