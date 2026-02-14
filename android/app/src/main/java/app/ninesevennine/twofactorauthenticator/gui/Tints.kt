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
        background = Color(0xFF4E4E4E),
        light = Color(0xFF2E2E2E),
        medium = Color(0xFF6E6E6E),
        dark = Color(0xFFCCCCCC)
    )
}