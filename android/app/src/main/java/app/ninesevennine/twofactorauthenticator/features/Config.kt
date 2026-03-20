package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Config {
    private const val PREFS_NAME = "config"
    private const val KEY_LOCALE = "locale"
    private const val KEY_THEME = "theme"

    var activeIconPackId by mutableStateOf("")
    var hasCompletedSetup: Boolean = false
    var isUsingMinimalisticTiles by mutableStateOf(false)
    var isUsingSearchOnTop by mutableStateOf(true)
    var isUsingTapToRevealCodes by mutableStateOf(false)
    var isUsingDoubleTapToCopyCodes by mutableStateOf(false)
    var isUsingFocusSearch by mutableStateOf(false)
    var isUsingBiometrics by mutableStateOf(false)
    var isUsingScreenSecurity by mutableStateOf(false)
    var isUsingSecureClipboard by mutableStateOf(false)
    var isUsingBlockIntents by mutableStateOf(false)
    var isUsingAutomaticBackups by mutableStateOf(false)
    var UsingIconPacksStyle by mutableIntStateOf(IconPacksStyle.TINTED.value)
    var isUsingIconPacksDontSuggestColors by mutableStateOf(false)

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

        activeIconPackId = prefs.getString("active_icon_pack_id", "") ?: ""
        hasCompletedSetup = prefs.getBoolean("has_completed_setup", false)
        isUsingMinimalisticTiles = prefs.getBoolean("is_using_minimalistic_tiles", false)
        isUsingSearchOnTop = prefs.getBoolean("is_using_search_on_top", true)
        isUsingTapToRevealCodes = prefs.getBoolean("is_using_tap_to_reveal_codes", false)
        isUsingDoubleTapToCopyCodes = prefs.getBoolean("is_using_double_tap_to_copy_codes", false)
        isUsingFocusSearch = prefs.getBoolean("is_using_focus_search", false)
        isUsingBiometrics = prefs.getBoolean("is_using_biometrics", false)
        isUsingScreenSecurity = prefs.getBoolean("is_using_screen_security", false)
        isUsingSecureClipboard = prefs.getBoolean("is_using_secure_clipboard", false)
        isUsingBlockIntents = prefs.getBoolean("is_using_block_intents", false)
        isUsingAutomaticBackups = prefs.getBoolean("is_using_automatic_internal_backups", false)
        UsingIconPacksStyle = prefs.getInt("using_iconpacks_style", IconPacksStyle.TINTED.value)
        isUsingIconPacksDontSuggestColors = prefs.getBoolean("is_using_iconpacks_dont_suggest_colors", false)

        IconPacks.load(context, activeIconPackId)
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit().apply {
            putString(KEY_LOCALE, Locale.localeOption.tag)
            putInt(KEY_THEME, Theme.themeOption.value)
            putString("active_icon_pack_id", activeIconPackId)
            putBoolean("has_completed_setup", hasCompletedSetup)
            putBoolean("is_using_minimalistic_tiles", isUsingMinimalisticTiles)
            putBoolean("is_using_search_on_top", isUsingSearchOnTop)
            putBoolean("is_using_tap_to_reveal_codes", isUsingTapToRevealCodes)
            putBoolean("is_using_double_tap_to_copy_codes", isUsingDoubleTapToCopyCodes)
            putBoolean("is_using_focus_search", isUsingFocusSearch)
            putBoolean("is_using_biometrics", isUsingBiometrics)
            putBoolean("is_using_screen_security", isUsingScreenSecurity)
            putBoolean("is_using_secure_clipboard", isUsingSecureClipboard)
            putBoolean("is_using_block_intents", isUsingBlockIntents)
            putBoolean("is_using_automatic_internal_backups", isUsingAutomaticBackups)
            putInt("using_iconpacks_style", UsingIconPacksStyle)
            putBoolean("is_using_iconpacks_dont_suggest_colors", isUsingIconPacksDontSuggestColors)
            apply()
        }
    }
}