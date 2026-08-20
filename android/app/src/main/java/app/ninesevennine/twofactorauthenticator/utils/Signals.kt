package app.ninesevennine.twofactorauthenticator.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Signals {
    var authenticate by mutableIntStateOf(0)
    var accountAdded by mutableStateOf(false)
    var suppressLockOnExit = false
    var suppressFocusRequestOnLaunch = false
}