package app.ninesevennine.twofactorauthenticator.gui

import androidx.compose.ui.graphics.Color

data class ColorFilters(
    val background: Color = Color(0),
    val light: Color = Color(0),
    val medium: Color = Color(0),
    val dark: Color = Color(0),
)

object Tints {
    val light = ColorFilters(
        background = Color(0xFFD7D7D7),
        light = Color(0xFFF2F2F2),
        medium = Color(0xFFBDBDBD),
        dark = Color(0xFF4A4A4A)
    )

    val dark = ColorFilters(
        background = Color(0xFF5A5A5A),
        light = Color(0xFF3C3C3C),
        medium = Color(0xFF808080),
        dark = Color(0xFFE0E0E0)
    )
}