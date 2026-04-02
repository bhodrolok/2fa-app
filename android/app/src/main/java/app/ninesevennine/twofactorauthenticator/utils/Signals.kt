package app.ninesevennine.twofactorauthenticator.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Signals {
    var requireAuthentication by mutableStateOf(false)
    var checkEditAccountSome by mutableStateOf(false)
    var accountAdded by mutableStateOf(false)
    var suppressLockOnExit = false
}