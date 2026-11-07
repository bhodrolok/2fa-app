package app.ninesevennine.twofactorauthenticator.features

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.ConfigurationCompat
import java.util.Locale

enum class LocaleOption(val tag: String, val displayName: String) {
    SYSTEM("", "Use System Default"),
    CHINESE_SIMPLIFIED("zh-CN", "简体中文"),
    CHINESE_TRADITIONAL("zh-TW", "繁體中文"),
    DUTCH("nl", "Nederlands"),
    ENGLISH("en", "English (United States)"),
    ESTONIAN("et", "Eesti"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    RUSSIAN("ru", "Русский"),
    SPANISH("es", "Español"),
    TAMAZIGHT("tzm", "Tamaziɣt"),
    UKRAINIAN("uk", "Українська");

    companion object {
        fun fromLanguage(tag: String): LocaleOption {
            val wanted = Locale.forLanguageTag(tag)
            val options = entries.filter { it != SYSTEM }

            return options.firstOrNull {
                val option = Locale.forLanguageTag(it.tag)
                option.language == wanted.language && option.country == wanted.country
            } ?: options.firstOrNull {
                Locale.forLanguageTag(it.tag).language == wanted.language
            } ?: SYSTEM
        }
    }
}

object Locale {
    private var _localeOption by mutableStateOf(LocaleOption.SYSTEM)

    val localeOption: LocaleOption get() = _localeOption

    private fun getLocaleManager(context: Context): LocaleManager {
        return context.getSystemService(LocaleManager::class.java)
    }

    // i'm not wasting time on this, using the old method from 2fa-android
    // doesn't update rtl, users can live with the black flickering.
    fun set(context: Context, value: LocaleOption) {
        _localeOption = value

        val tag = when (value) {
            LocaleOption.SYSTEM -> {
                val deviceLocales = LocaleList.getDefault()
                (1 until deviceLocales.size())
                    .map { LocaleOption.fromLanguage(deviceLocales[it].toLanguageTag()) }
                    .firstOrNull { it != LocaleOption.SYSTEM }
                    ?.tag
                    ?: LocaleOption.ENGLISH.tag
            }
            else -> value.tag
        }

        getLocaleManager(context).applicationLocales = LocaleList.forLanguageTags(tag)
    }

    fun currentEffectiveLocale(context: Context): LocaleOption {
        val activeLocale = ConfigurationCompat.getLocales(context.resources.configuration).get(0)
            ?: return LocaleOption.ENGLISH

        return LocaleOption.fromLanguage(activeLocale.toLanguageTag())
    }
}