package com.oxygenupdater.ui

import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

val currentLocale: Locale
    get() = AppCompatDelegate.getApplicationLocales()[0] ?: LocaleListCompat.getAdjustedDefault()[0]!!

/**
 * We send full language tags only if the language in question is region-qualified in the app.
 * This avoids polluting cache with unnecessary language variant info. For example, there will
 * always be only one "English" or "Dutch" supported in the app.
 */
inline val currentLanguage: String
    get() = currentLocale.languageOrLanguageTag()

@Composable
@ReadOnlyComposable
fun currentLocale() = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: LocaleListCompat.getAdjustedDefault()[0]!!

/**
 * This is intentionally simple for performance.
 *
 * Its behaviour is fully-tested in [com.oxygenupdater.ui.CurrentLocaleTest].
 */
@VisibleForTesting
inline fun Locale.languageOrLanguageTag() = language.let {
    if (it == "pt" || it == "zh") toLanguageTag() else it
} ?: "en" // fallback just in case
