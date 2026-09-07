package app.ninesevennine.twofactorauthenticator.utils

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.ninesevennine.twofactorauthenticator.features.Locale
import app.ninesevennine.twofactorauthenticator.features.LocaleOption
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.ui.screens.OtpCardStyle

object Config {
    private const val PREFS_NAME = "config"
    private const val KEY_LOCALE = "locale"
    private const val KEY_THEME = "theme"

    var isUsingTexturedIcons by mutableStateOf(true)
    var isUsingIconPacks by mutableStateOf(true)
    var UsingOtpCardStyle by mutableIntStateOf(OtpCardStyle.NORMAL.value)
    var isUsingTapToRevealCodes by mutableStateOf(false)
    var isUsingDoubleTapToCopyCodes by mutableStateOf(false)
    var isUsingFocusSearchOnLaunch by mutableStateOf(false)
    var isUsingScreenSecurity by mutableStateOf(false)
    var isUsingSecureClipboard by mutableStateOf(false)
    var isUsingLockOnExit by mutableStateOf(false)
    var isUsingBlockIntents by mutableStateOf(false)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val localeTag = prefs.getString(KEY_LOCALE, LocaleOption.SYSTEM.tag)
            ?: LocaleOption.SYSTEM.tag
        val locale = LocaleOption.fromLanguage(localeTag)

        val themeInt = prefs.getInt(KEY_THEME, ThemeOption.SYSTEM.value)
        val theme = ThemeOption.entries.firstOrNull { it.value == themeInt }
            ?: ThemeOption.SYSTEM

        Locale.set(context, locale)
        Theme.set(context, theme)

        isUsingTexturedIcons = prefs.getBoolean("is_using_textured_icons", true)
        isUsingIconPacks = prefs.getBoolean("is_using_icon_packs", true)
        UsingOtpCardStyle = prefs.getInt("using_otp_card_style", OtpCardStyle.NORMAL.value)
        isUsingTapToRevealCodes = prefs.getBoolean("is_using_tap_to_reveal_codes", false)
        isUsingDoubleTapToCopyCodes = prefs.getBoolean("is_using_double_tap_to_copy_codes", false)
        isUsingFocusSearchOnLaunch = prefs.getBoolean("is_using_focus_search", false)
        isUsingScreenSecurity = prefs.getBoolean("is_using_screen_security", false)
        isUsingSecureClipboard = prefs.getBoolean("is_using_secure_clipboard", false)
        isUsingLockOnExit = prefs.getBoolean("is_using_lock_on_exit", false)
        isUsingBlockIntents = prefs.getBoolean("is_using_block_intents", false)
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit().apply {
            putString(KEY_LOCALE, Locale.localeOption.tag)
            putInt(KEY_THEME, Theme.themeOption.value)
            putBoolean("is_using_textured_icons", isUsingTexturedIcons)
            putBoolean("is_using_icon_packs", isUsingIconPacks)
            putInt("using_otp_card_style", UsingOtpCardStyle)
            putBoolean("is_using_tap_to_reveal_codes", isUsingTapToRevealCodes)
            putBoolean("is_using_double_tap_to_copy_codes", isUsingDoubleTapToCopyCodes)
            putBoolean("is_using_focus_search", isUsingFocusSearchOnLaunch)
            putBoolean("is_using_screen_security", isUsingScreenSecurity)
            putBoolean("is_using_secure_clipboard", isUsingSecureClipboard)
            putBoolean("is_using_lock_on_exit", isUsingLockOnExit)
            putBoolean("is_using_block_intents", isUsingBlockIntents)
            apply()
        }
    }
}