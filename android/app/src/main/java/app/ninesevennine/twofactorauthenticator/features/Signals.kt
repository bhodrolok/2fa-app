package app.ninesevennine.twofactorauthenticator.features

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object Signals {
    var intent by mutableIntStateOf(0)
}