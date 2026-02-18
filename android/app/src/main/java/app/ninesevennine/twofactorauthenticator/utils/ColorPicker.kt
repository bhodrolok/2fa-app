package app.ninesevennine.twofactorauthenticator.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

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

    fun fromHex(hex: String): Color {
        val clean = hex.trimStart('#')
        val padded = when (clean.length) {
            6 -> "FF$clean"
            8 -> clean
            else -> return Color.White
        }
        return runCatching {
            Color(padded.toLong(16).toInt())
        }.getOrDefault(Color.White)
    }
}