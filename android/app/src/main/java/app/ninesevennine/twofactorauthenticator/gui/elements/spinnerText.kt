package app.ninesevennine.twofactorauthenticator.gui.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private val spinnerFrames = arrayOf("|", "/", "-", "\\")

@Composable
fun spinnerText(): String {
    var frameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            frameIndex = (frameIndex + 1) % spinnerFrames.size
        }
    }

    return spinnerFrames[frameIndex]
}