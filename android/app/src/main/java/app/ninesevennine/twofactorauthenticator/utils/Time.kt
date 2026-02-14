package app.ninesevennine.twofactorauthenticator.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Clock

object Time {
    var epochSeconds by mutableLongStateOf(0)

    @Composable
    fun Track() {
        LaunchedEffect(Unit) {
            while (true) {
                val now = Clock.System.now()
                epochSeconds = now.epochSeconds

                val millisUntilNextSecond = 1000 - (now.toEpochMilliseconds() % 1000)
                delay(millisUntilNextSecond)
            }
        }
    }
}