package app.ninesevennine.twofactorauthenticator.features

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object Signals {
    var authenticate by mutableIntStateOf(0)
    var intent by mutableIntStateOf(0)
    var suppressLockOnExit = false
}