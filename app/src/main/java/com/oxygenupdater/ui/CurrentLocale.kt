package com.oxygenupdater.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

val currentLocale: Locale
    get() = AppCompatDelegate.getApplicationLocales()[0] ?: LocaleListCompat.getAdjustedDefault()[0]!!

inline val currentLanguage: String
    get() = currentLocale.run {
        // We send full language tags only if the language in question is region-qualified in the app.
        // This avoids polluting cache with unnecessary language variant info. For example, there will
        // always be only one "English" or "Dutch" supported in the app.
        // Note: this should be updated when the app adds more languages with region qualifiers.
        val language = language
        if (language == "pt" || language == "zh") toLanguageTag() else language
    }

@Composable
@ReadOnlyComposable
fun currentLocale() = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: LocaleListCompat.getAdjustedDefault()[0]!!
