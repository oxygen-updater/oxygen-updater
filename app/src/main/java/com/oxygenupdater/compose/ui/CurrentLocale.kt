package com.oxygenupdater.compose.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

val currentLocale: Locale
    get() = AppCompatDelegate.getApplicationLocales()[0] ?: LocaleListCompat.getAdjustedDefault()[0]!!

@Composable
@ReadOnlyComposable
fun currentLocale() = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: LocaleListCompat.getAdjustedDefault()[0]!!
