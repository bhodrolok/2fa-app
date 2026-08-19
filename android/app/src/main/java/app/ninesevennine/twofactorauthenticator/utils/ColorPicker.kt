package app.ninesevennine.twofactorauthenticator.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt

object ColorPicker {
    fun fromHexString(hex: String): Color?  {
        val formattedHex = if (hex.startsWith("#")) hex else "#$hex"

        return try {
            Color(formattedHex.toColorInt())
        } catch (_: Exception) {
            null
        }
    }
}