package app.ninesevennine.twofactorauthenticator.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt

object ColorPicker {
    fun colorToHsl(color: Color): FloatArray {
        return FloatArray(3).also { ColorUtils.colorToHSL(color.toArgb(), it) }
    }

    fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
        return Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness)))
    }

    fun toRgbHexString(color: Color): String {
        return "#%06x".format(color.toArgb() and 0xFFFFFF)
    }

    fun fromHexString(hex: String): Color?  {
        val formattedHex = if (hex.startsWith("#")) hex else "#$hex"

        return try {
            Color(formattedHex.toColorInt())
        } catch (_: Exception) {
            null
        }
    }
}