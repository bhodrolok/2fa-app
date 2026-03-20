package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import android.content.res.Configuration
import android.view.Window
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowInsetsControllerCompat

enum class ThemeOption(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    PITCH_BLACK(3);
}

data class ThemeColors(
    val background: Color = Color(0),
    val surface: Color = Color(0),
    val primary: Color = Color(0),
    val secondary: Color = Color(0),
    val border: Color = Color(0),
    val veryWeak: Color = Color(0),
    val weak: Color = Color(0),
    val good: Color = Color(0),
    val strong: Color = Color(0),
    val info: Color = Color(0)
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
        secondary = Color(0xFF6A6A6A),
        border = Color(0xFFBFBFBF),
        veryWeak = Color(0xFFD32F2F),
        weak = Color(0xFFFBC02D),
        good = Color(0xFF388E3C),
        strong = Color(0xFF1976D2),
        info = Color(0xFF1976D2)
    )

    private val dark = ThemeColors(
        background = Color(0xFF060606),
        surface = Color(0xFF121212),
        primary = Color(0xFFDEDEDA),
        secondary = Color(0xFF9A9A96),
        border = Color(0xFF1E1D1C),
        veryWeak = Color(0xFFFF5252),
        weak = Color(0xFFFFD740),
        good = Color(0xFF66BB6A),
        strong = Color(0xFF42A5F5),
        info = Color(0xFF42A5F5)
    )

    private val pitchBlack = ThemeColors(
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0A),
        primary = Color(0xFFE4E4E0),
        secondary = Color(0xFF8A8A86),
        border = Color(0xFF1A1918),
        veryWeak = Color(0xFFFF5252),
        weak = Color(0xFFFFD740),
        good = Color(0xFF66BB6A),
        strong = Color(0xFF42A5F5),
        info = Color(0xFF42A5F5)
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