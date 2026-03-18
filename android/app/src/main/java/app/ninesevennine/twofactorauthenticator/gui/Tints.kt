package app.ninesevennine.twofactorauthenticator.gui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import app.ninesevennine.twofactorauthenticator.utils.ColorPicker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ColorTints(
    val light: Color = Color(0),
    val medium: Color = Color(0),
    val dark: Color = Color(0),
)

object Tints {
    val light = ColorTints(
        light = Color(0xFFF2F2F2),
        medium = Color(0xFFBDBDBD),
        dark = Color(0xFF4A4A4A)
    )

    val dark = ColorTints(
        light = Color(0xFF3C3C3C),
        medium = Color(0xFF808080),
        dark = Color(0xFFE0E0E0)
    )

    fun lightFromColor(color: Color): ColorTints {
        val oklab = color.convert(ColorSpaces.Oklab)
        val L = oklab.component1()
        val a = oklab.component2()
        val b = oklab.component3()

        val chroma = sqrt(a * a + b * b)
        val hue = atan2(b, a)

        // Background — light but visibly colored
        val lightL = 0.95f
        val lightC = chroma * 0.35f

        // Code area — the brand color at its natural lightness
        val medL = L.coerceIn(0.40f, 0.65f)
        val medC = chroma

        // Text/icons — dark tint of the color
        val darkL = 0.38f
        val darkC = chroma * 0.30f

        return ColorTints(
            light  = oklchToSrgb(lightL, lightC, hue),
            medium = oklchToSrgb(medL, medC, hue),
            dark   = oklchToSrgb(darkL, darkC, hue),
        )
    }

    fun darkFromColor(color: Color): ColorTints {
        val oklab = color.convert(ColorSpaces.Oklab)
        val L = oklab.component1()
        val a = oklab.component2()
        val b = oklab.component3()

        val chroma = sqrt(a * a + b * b)
        val hue = atan2(b, a)

        // Background — dark and visibly colored
        val lightL = 0.25f
        val lightC = chroma * 0.35f

        // Code area — brand color pushed dark
        val medL = L.coerceIn(0.35f, 0.55f)
        val medC = chroma

        // Text/icons — light tint of the color
        val darkL = 0.88f
        val darkC = chroma * 0.50f

        return ColorTints(
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