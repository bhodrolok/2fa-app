package app.ninesevennine.twofactorauthenticator.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Signals {
    var requiresAuthentication = false
    var authenticate by mutableIntStateOf(0)
    var checkHostages by mutableStateOf(false)
    var suppressLockOnExit = false
    var suppressFocusSearchOnLaunch = false
}