package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Config {
    private const val PREFS_NAME = "config"
    private const val KEY_LOCALE = "locale"
    private const val KEY_THEME = "theme"

    var activeIconPackId by mutableStateOf("")
    var internalIsUsingKeyStoreAES: Boolean = false
    var hasSeenKeyStoreMessage: Boolean = false
    var hasCompletedSetup: Boolean = false
    var isUsingMinimalisticTiles by mutableStateOf(false)
    var isUsingSearchOnTop by mutableStateOf(true)
    var isUsingTapToRevealCodes by mutableStateOf(false)
    var isUsingDoubleTapToCopyCodes by mutableStateOf(false)
    var isUsingFocusSearch by mutableStateOf(false)
    var isUsingBiometrics by mutableStateOf(false)
    var isUsingScreenSecurity by mutableStateOf(true)
    var isUsingBlockIntents by mutableStateOf(false)
    var isUsingAutomaticInternalBackups by mutableStateOf(false)
    var isUsingIconPacksOverrideColors by mutableStateOf(false)
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

        IconPacks.load(context, activeIconPackId)

        internalIsUsingKeyStoreAES = prefs.getBoolean("internal_is_using_keystore_aes", false)
        hasCompletedSetup = prefs.getBoolean("has_completed_setup", false)
        isUsingMinimalisticTiles = prefs.getBoolean("is_using_minimalistic_tiles", false)
        isUsingSearchOnTop = prefs.getBoolean("is_using_search_on_top", true)
        isUsingTapToRevealCodes = prefs.getBoolean("is_using_tap_to_reveal_codes", false)
        isUsingDoubleTapToCopyCodes = prefs.getBoolean("is_using_double_tap_to_copy_codes", false)
        isUsingFocusSearch = prefs.getBoolean("is_using_focus_search", false)
        isUsingBiometrics = prefs.getBoolean("is_using_biometrics", false)
        isUsingScreenSecurity = prefs.getBoolean("is_using_screen_security", true)
        isUsingBlockIntents = prefs.getBoolean("is_using_block_intents", false)
        isUsingAutomaticInternalBackups = prefs.getBoolean("is_using_automatic_internal_backups", false)
        isUsingIconPacksOverrideColors = prefs.getBoolean("is_using_iconpacks_override_colors", false)
        isUsingIconPacksDontSuggestColors = prefs.getBoolean("is_using_iconpacks_dont_suggest_colors", false)
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit().apply {
            putString(KEY_LOCALE, Locale.localeOption.tag)
            putInt(KEY_THEME, Theme.themeOption.value)
            putString("active_icon_pack_id", activeIconPackId)
            putBoolean("internal_is_using_keystore_aes", internalIsUsingKeyStoreAES)
            putBoolean("has_completed_setup", hasCompletedSetup)
            putBoolean("is_using_minimalistic_tiles", isUsingMinimalisticTiles)
            putBoolean("is_using_search_on_top", isUsingSearchOnTop)
            putBoolean("is_using_tap_to_reveal_codes", isUsingTapToRevealCodes)
            putBoolean("is_using_double_tap_to_copy_codes", isUsingDoubleTapToCopyCodes)
            putBoolean("is_using_focus_search", isUsingFocusSearch)
            putBoolean("is_using_biometrics", isUsingBiometrics)
            putBoolean("is_using_screen_security", isUsingScreenSecurity)
            putBoolean("is_using_block_intents", isUsingBlockIntents)
            putBoolean("is_using_automatic_internal_backups", isUsingAutomaticInternalBackups)
            putBoolean("is_using_iconpacks_override_colors", isUsingIconPacksOverrideColors)
            putBoolean("is_using_iconpacks_dont_suggest_colors", isUsingIconPacksDontSuggestColors)
            apply()
        }
    }
}