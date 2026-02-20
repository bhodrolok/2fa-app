package app.ninesevennine.twofactorauthenticator.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt

object ColorPicker {
    val hueGradient = listOf(
        Color(0xFFFF0000),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF00FFFF),
        Color(0xFF0000FF),
        Color(0xFFFF00FF),
        Color(0xFFFF0000),
    )

    fun fromHsv(h: Float, s: Float, v: Float): Color =
        Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))

    fun toHsv(color: Color): FloatArray =
        FloatArray(3).also {
            android.graphics.Color.RGBToHSV(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt(),
                it,
            )
        }

    fun toHex(color: Color): String =
        "#%06X".format(color.toArgb() and 0xFFFFFF)

    fun fromHex(hex: String): Color?  {
        return try {
            Color(hex.toColorInt())
        } catch (_: Exception) {
            null
        }
    }
}