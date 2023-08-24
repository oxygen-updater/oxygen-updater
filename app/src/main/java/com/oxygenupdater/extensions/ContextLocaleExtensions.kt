package com.oxygenupdater.extensions

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.oxygenupdater.internal.settings.PrefManager
import java.util.*

/**
 * This file can't use any Koin-managed singletons, because most functions
 * here can be called before [android.app.Application.onCreate]
 * (which is where Koin is initialized)
 */

fun String.toLocale() = split("-r", limit = 2).let {
    val language = it[0]
    val country = it.getOrElse(1) { "" }
    Locale(language, country)
}

fun Locale.toLanguageCode(): String = let {
    val language = it.language
    // Add other languages here if they need country-specifications too
    if (language == "pt" || language == "zh") "$language-r${it.country}" else language
}

fun Context.attachWithLocale(persist: Boolean = true) = persistAndSetLocale(
    PreferenceManager.getDefaultSharedPreferences(this),
    persist
)

fun Context.setLocale(languageCode: String) = languageCode.toLocale().let {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocaleList.setDefault(LocaleList(it))
    } else {
        Locale.setDefault(it)
    }
    updateResources(it)
}

private fun Context.persistAndSetLocale(
    sharedPreferences: SharedPreferences,
    persist: Boolean,
) = sharedPreferences.getString(
    PrefManager.PROPERTY_LANGUAGE_ID,
    Locale.getDefault().toLanguageCode()
)!!.let { languageCode ->
    if (persist) {
        sharedPreferences.edit {
            putString(PrefManager.PROPERTY_LANGUAGE_ID, languageCode)
        }
    }

    setLocale(languageCode)
}

private fun Context.updateResources(
    locale: Locale,
): Context = resources.configuration.let { config ->
    config.setLocale(locale)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        createConfigurationContext(config)
    } else {
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        this
    }
}
