package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import android.content.res.Configuration
import android.view.Window
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowInsetsControllerCompat

enum class ThemeOption(val value: Int, val displayName: String) {
    SYSTEM(0, "Use System Default"),
    LIGHT(1, "Light"),
    DARK(2, "Dark"),
    PITCH_BLACK(3, "Pitch Black");
}

data class ThemeColors(
    val background: Color = Color(0),
    val surface: Color = Color(0),
    val primary: Color = Color(0),
    val secondary: Color = Color(0),
    val border: Color = Color(0),
    val info: Color = Color(0),
    val warning: Color = Color(0),
    val error: Color = Color(0),
)

object Theme {
    var window: Window? = null

    private var _themeOption by mutableStateOf(ThemeOption.SYSTEM)
    private var _colors by mutableStateOf(ThemeColors())

    val colors: ThemeColors get() = _colors
    val themeOption: ThemeOption get() = _themeOption

    private val light = ThemeColors(
        background = Color(0xFFFCFCFC),
        surface = Color(0xFFFFFFFF),
        primary = Color(0xFF282828),
        secondary = Color(0xFF4A4A4A),
        border = Color(0xFFBFBFBF),
        info = Color(0xFF0288D1),
        warning = Color(0xFFED6C02),
        error = Color(0xFFD32F2F)
    )

    private val dark = ThemeColors(
        background = Color(0xFF060606),
        surface = Color(0xFF0B0B0B),
        primary = Color(0xFFDEDEDA),
        secondary = Color(0xFF817E77),
        border = Color(0xFF121110),
        info = Color(0xFF03A9F4),
        warning = Color(0xFFFFA000),
        error = Color(0xFFE53935)
    )

    private val pitchBlack = ThemeColors(
        background = Color.Black,
        surface = Color.Black,
        primary = Color.White,
        secondary = Color.White,
        border = Color.White,
        info = Color.Cyan,
        warning = Color.Yellow,
        error = Color.Red
    )

    private fun isSystemDark(context: Context): Boolean {
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    fun set(context: Context, value: ThemeOption) {
        _themeOption = value
        _colors = when (value) {
            ThemeOption.SYSTEM -> if (isSystemDark(context)) dark else light
            ThemeOption.LIGHT -> light
            ThemeOption.DARK -> dark
            ThemeOption.PITCH_BLACK -> pitchBlack
        }

        update(context)
    }

    private fun applyBarAppearance(isLight: Boolean) {
        window?.let {
            val controller = WindowInsetsControllerCompat(it, it.decorView)
            controller.isAppearanceLightStatusBars = isLight
            controller.isAppearanceLightNavigationBars = isLight
        }
    }

    fun update(context: Context) {
        when (themeOption) {
            ThemeOption.SYSTEM -> {
                val darkMode = isSystemDark(context)
                _colors = if (darkMode) dark else light
                applyBarAppearance(!darkMode)
            }

            ThemeOption.LIGHT -> applyBarAppearance(true)
            ThemeOption.DARK, ThemeOption.PITCH_BLACK -> applyBarAppearance(false)
        }
    }

    fun currentOption(context: Context): ThemeOption {
        val saved = _themeOption
        if (saved != ThemeOption.SYSTEM) return saved
        return if (isSystemDark(context)) ThemeOption.DARK else ThemeOption.LIGHT
    }
}