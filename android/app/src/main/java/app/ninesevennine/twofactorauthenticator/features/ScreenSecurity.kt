package app.ninesevennine.twofactorauthenticator.features

import android.app.Activity
import android.view.WindowManager

fun setScreenSecure(activity: Activity, isSecure: Boolean) {
    if (isSecure) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}