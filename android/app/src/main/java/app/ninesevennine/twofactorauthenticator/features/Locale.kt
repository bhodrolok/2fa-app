package app.ninesevennine.twofactorauthenticator.features

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.ConfigurationCompat

enum class LocaleOption(val tag: String, val displayName: String) {
    SYSTEM("", "Use System Default"),
    ENGLISH("en", "English International"),
    SPANISH("es", "Español"),
    RUSSIAN("ru", "Русский"),
    GERMAN("de", "Deutsch"),
    UKRAINIAN("uk", "Українська"),
    DUTCH("nl", "Nederland"),
    ESTONIAN("et", "Eesti");

    companion object {
        fun fromLanguage(language: String): LocaleOption {
            return entries.firstOrNull { it.tag == language } ?: SYSTEM
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
        val localeManager = getLocaleManager(context)

        when (value) {
            LocaleOption.SYSTEM -> {
                val deviceLocales: LocaleList = LocaleList.getDefault()

                if (deviceLocales.size() > 0) {
                    for (i in 1 until deviceLocales.size()) {
                        val deviceLang = deviceLocales[i].language

                        val match = LocaleOption.entries.filter { it.tag.isNotEmpty() }.firstOrNull { it.tag == deviceLang }

                        if (match != null && match.tag.isNotEmpty()) {
                            localeManager.applicationLocales = LocaleList.forLanguageTags(match.tag)
                            return
                        }
                    }
                }

                localeManager.applicationLocales = LocaleList.forLanguageTags(LocaleOption.ENGLISH.tag)
            }

            else -> {
                localeManager.applicationLocales = LocaleList.forLanguageTags(value.tag)
            }
        }
    }

    fun currentEffectiveLocale(context: Context): LocaleOption {
        val activeLocale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
            ?: return LocaleOption.ENGLISH

        return LocaleOption.fromLanguage(activeLocale.language)
    }
}