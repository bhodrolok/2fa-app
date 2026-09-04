package app.ninesevennine.twofactorauthenticator.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class OtpCardColors(
    val background: Color = Color(0),
    val surface: Color = Color(0),
    val text: Color = Color(0),
    val border: Color = Color(0),
)

object OtpCard {
    fun lightFromColor(color: Color): OtpCardColors {
        val oklab = color.convert(ColorSpaces.Oklab)
        val l = oklab.component1()
        val a = oklab.component2()
        val b = oklab.component3()

        val chroma = sqrt(a * a + b * b)
        val hue = atan2(b, a)

        val backgroundL = 0.97f
        val backgroundC = chroma * 0.30f

        val surfaceL = l.coerceIn(0.40f, 0.65f)
        val surfaceC = chroma

        val textL = 0.38f
        val textC = chroma * 0.30f

        val borderL = 0.88f
        val borderC = chroma * 0.45f

        return OtpCardColors(
            background = oklchToSrgb(backgroundL, backgroundC, hue),
            surface = oklchToSrgb(surfaceL, surfaceC, hue),
            text = oklchToSrgb(textL, textC, hue),
            border = oklchToSrgb(borderL, borderC, hue),
        )
    }

    fun darkFromColor(color: Color): OtpCardColors {
        val oklab = color.convert(ColorSpaces.Oklab)
        val l = oklab.component1()
        val a = oklab.component2()
        val b = oklab.component3()

        val chroma = sqrt(a * a + b * b)
        val hue = atan2(b, a)

        val backgroundL = 0.20f
        val backgroundC = chroma * 0.30f

        val surfaceL = l.coerceIn(0.35f, 0.55f)
        val surfaceC = chroma

        val textL = 0.88f
        val textC = chroma * 0.50f

        val borderL = 0.35f
        val borderC = chroma * 0.50f

        return OtpCardColors(
            background = oklchToSrgb(backgroundL, backgroundC, hue),
            surface = oklchToSrgb(surfaceL, surfaceC, hue),
            text = oklchToSrgb(textL, textC, hue),
            border = oklchToSrgb(borderL, borderC, hue),
        )
    }

    val pitchBlack = OtpCardColors(
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0A),
        text = Color(0xFFE6E6E2),
        border = Color(0xFF161514),
    )

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