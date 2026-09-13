package app.ninesevennine.twofactorauthenticator.utils

import android.annotation.SuppressLint
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavOptionsBuilder

@SuppressLint("RestrictedApi")
fun NavController.navigateOnce(route: Any, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (currentDestination?.hasRoute(route::class) == false) {
        navigate(route) {
            launchSingleTop = true
            builder()
        }
    }
}