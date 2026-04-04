package app.ninesevennine.twofactorauthenticator.features

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale as JavaLocale

enum class LocaleOption(val tag: String, val displayName: String) {
    SYSTEM("", "Use System Default"),
    ENGLISH("en", "English International"),
    SPANISH("es", "Español"),
    RUSSIAN("ru", "Русский");

    companion object {
        fun fromLanguage(language: String): LocaleOption {
            return entries.firstOrNull { it.tag == language } ?: ENGLISH
        }
    }
}

object Locale {
    private var _localeOption by mutableStateOf(LocaleOption.ENGLISH)

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
                localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
            }

            else -> {
                localeManager.applicationLocales = LocaleList.forLanguageTags(value.tag)
            }
        }
    }

    fun currentEffectiveLocale(context: Context): LocaleOption {
        val localeManager = getLocaleManager(context)
        val appLocales = localeManager.applicationLocales

        val effectiveLocale: JavaLocale = if (!appLocales.isEmpty) {
            appLocales[0]
        } else {
            JavaLocale.getDefault()
        }

        return LocaleOption.fromLanguage(effectiveLocale.language)
    }

    fun currentDisplayName(): String {
        if (_localeOption != LocaleOption.SYSTEM) {
            return _localeOption.displayName
        }

        val systemLocale = JavaLocale.getDefault()
        val matched = LocaleOption.fromLanguage(systemLocale.language)

        return if (matched != LocaleOption.SYSTEM) {
            matched.displayName
        } else {
            LocaleOption.ENGLISH.displayName
        }
    }
}