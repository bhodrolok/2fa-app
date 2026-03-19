package app.ninesevennine.twofactorauthenticator.gui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class TileColors(
    val light: Color = Color(0),
    val medium: Color = Color(0),
    val dark: Color = Color(0),
)

object OtpTileColors {
    fun lightFromColor(color: Color): TileColors {
        val oklab = color.convert(ColorSpaces.Oklab)
        val l = oklab.component1()
        val a = oklab.component2()
        val b = oklab.component3()

        val chroma = sqrt(a * a + b * b)
        val hue = atan2(b, a)

        val lightL = 0.95f
        val lightC = chroma * 0.35f

        val medL = l.coerceIn(0.40f, 0.65f)
        val medC = chroma

        val darkL = 0.38f
        val darkC = chroma * 0.30f

        return TileColors(
            light  = oklchToSrgb(lightL, lightC, hue),
            medium = oklchToSrgb(medL, medC, hue),
            dark   = oklchToSrgb(darkL, darkC, hue),
        )
    }

    fun darkFromColor(color: Color): TileColors {
        val oklab = color.convert(ColorSpaces.Oklab)
        val l = oklab.component1()
        val a = oklab.component2()
        val b = oklab.component3()

        val chroma = sqrt(a * a + b * b)
        val hue = atan2(b, a)

        val lightL = 0.25f
        val lightC = chroma * 0.35f

        val medL = l.coerceIn(0.35f, 0.55f)
        val medC = chroma

        val darkL = 0.88f
        val darkC = chroma * 0.50f

        return TileColors(
            light  = oklchToSrgb(lightL, lightC, hue),
            medium = oklchToSrgb(medL, medC, hue),
            dark   = oklchToSrgb(darkL, darkC, hue),
        )
    }

    private fun oklchToSrgb(L: Float, C: Float, hue: Float): Color {
        var c = C
        repeat(16) {
            val a = cos(hue) * c
            val b = sin(hue) * c
            val rgb = Color(L, a, b, 1f, ColorSpaces.Oklab).convert(ColorSpaces.Srgb)
            if (rgb.red in 0f..1f && rgb.green in 0f..1f && rgb.blue in 0f..1f) {
                return rgb
            }
            c *= 0.9f
        }
        return Color(L, 0f, 0f, 1f, ColorSpaces.Oklab).convert(ColorSpaces.Srgb)
    }
}