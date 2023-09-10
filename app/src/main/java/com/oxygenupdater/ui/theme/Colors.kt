/*
 * GENERATOR: https://m3.material.io/theme-builder#/custom
 *    COLORS: https://forums-images.oneplus.net/attachments/1260/1260743-f51b09eec9d7a879a914042a8aae8a89.jpg (https://community.oneplus.com/thread/1201086)
 *
 *   Primary: #f50514 (red)
 * Secondary: #00c5e6 (teal)
 *  Tertiary: #f2f0fa (lavender)
 *   Neutral: #ffffff (white)
 */

@file:Suppress("PrivatePropertyName")

package com.oxygenupdater.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.Color

private val md_theme_light_primary = Color(0xFF0061A4)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFD1E4FF)
private val md_theme_light_onPrimaryContainer = Color(0xFF001D36)
private val md_theme_light_inversePrimary = Color(0xFF9ECAFF)
private val md_theme_light_secondary = Color(0xFF535F70)
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_secondaryContainer = Color(0xFFD7E3F7)
private val md_theme_light_onSecondaryContainer = Color(0xFF101C2B)
private val md_theme_light_tertiary = Color(0xFF6B5778)
private val md_theme_light_onTertiary = Color(0xFFFFFFFF)
private val md_theme_light_tertiaryContainer = Color(0xFFF2DAFF)
private val md_theme_light_onTertiaryContainer = Color(0xFF251431)
private val md_theme_light_background = Color(0xFFFDFCFF)
private val md_theme_light_onBackground = Color(0xFF1A1C1E)
private val md_theme_light_surface = Color(0xFFFDFCFF)
private val md_theme_light_onSurface = Color(0xFF1A1C1E)
private val md_theme_light_surfaceVariant = Color(0xFFDFE2EB)
private val md_theme_light_onSurfaceVariant = Color(0xFF43474E)
private val md_theme_light_surfaceTint = Color(0xFF0061A4)
private val md_theme_light_inverseSurface = Color(0xFF2F3033)
private val md_theme_light_inverseOnSurface = Color(0xFFF1F0F4)
private val md_theme_light_error = Color(0xFFBA1A1A)
private val md_theme_light_errorContainer = Color(0xFFFFDAD6)
private val md_theme_light_onError = Color(0xFFFFFFFF)
private val md_theme_light_onErrorContainer = Color(0xFF410002)
private val md_theme_light_outline = Color(0xFF73777F)
private val md_theme_light_outlineVariant = Color(0xFFC3C7CF)
private val md_theme_light_scrim = Color(0xFF000000)
private val md_theme_light_shadow = Color(0xFF000000)

private val md_theme_dark_primary = Color(0xFF9ECAFF)
private val md_theme_dark_onPrimary = Color(0xFF003258)
private val md_theme_dark_primaryContainer = Color(0xFF00497D)
private val md_theme_dark_onPrimaryContainer = Color(0xFFD1E4FF)
private val md_theme_dark_inversePrimary = Color(0xFF0061A4)
private val md_theme_dark_secondary = Color(0xFFBBC7DB)
private val md_theme_dark_onSecondary = Color(0xFF253140)
private val md_theme_dark_secondaryContainer = Color(0xFF3B4858)
private val md_theme_dark_onSecondaryContainer = Color(0xFFD7E3F7)
private val md_theme_dark_tertiary = Color(0xFFD6BEE4)
private val md_theme_dark_onTertiary = Color(0xFF3B2948)
private val md_theme_dark_tertiaryContainer = Color(0xFF523F5F)
private val md_theme_dark_onTertiaryContainer = Color(0xFFF2DAFF)
private val md_theme_dark_background = Color(0xFF1A1C1E)
private val md_theme_dark_onBackground = Color(0xFFE2E2E6)
private val md_theme_dark_surface = Color(0xFF1A1C1E)
private val md_theme_dark_onSurface = Color(0xFFE2E2E6)
private val md_theme_dark_surfaceVariant = Color(0xFF43474E)
private val md_theme_dark_onSurfaceVariant = Color(0xFFC3C7CF)
private val md_theme_dark_surfaceTint = Color(0xFF9ECAFF)
private val md_theme_dark_inverseSurface = Color(0xFFE2E2E6)
private val md_theme_dark_inverseOnSurface = Color(0xFF1A1C1E)
private val md_theme_dark_error = Color(0xFFFFB4AB)
private val md_theme_dark_errorContainer = Color(0xFF93000A)
private val md_theme_dark_onError = Color(0xFF690005)
private val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
private val md_theme_dark_outline = Color(0xFF8D9199)
private val md_theme_dark_outlineVariant = Color(0xFF43474E)
private val md_theme_dark_scrim = Color(0xFF000000)
private val md_theme_dark_shadow = Color(0xFF000000)

private val seed = Color(0xFF2196F3)
private val positive = Color(0xFF4CAF50)
private val warn = Color(0xFFFF9800)
private val light_positive = Color(0xFF006D43)
private val light_onPositive = Color(0xFFFFFFFF)
private val light_positiveContainer = Color(0xFF72FCB4)
private val light_onPositiveContainer = Color(0xFF002111)
private val dark_positive = Color(0xFF52DF9A)
private val dark_onPositive = Color(0xFF003921)
private val dark_positiveContainer = Color(0xFF005232)
private val dark_onPositiveContainer = Color(0xFF72FCB4)
private val light_warn = Color(0XFF994704)
private val light_onWarn = Color(0XFFFFFFFF)
private val light_warnContainer = Color(0XFFFFDBC9)
private val light_onWarnContainer = Color(0XFF321200)
private val dark_warn = Color(0XFFFFB68C)
private val dark_onWarn = Color(0XFF532200)
private val dark_warnContainer = Color(0XFF753400)
private val dark_onWarnContainer = Color(0XFFFFDBC9)

val ColorScheme.backgroundVariant get() = surfaceColorAtElevation(NavigationBarDefaults.Elevation)

val ColorScheme.light get() = onError == Color.White
val ColorScheme.positive get() = if (light) light_positive else dark_positive
val ColorScheme.warn get() = if (light) light_warn else dark_warn

val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    inversePrimary = md_theme_light_inversePrimary,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    surfaceTint = md_theme_light_surfaceTint,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    inversePrimary = md_theme_dark_inversePrimary,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    surfaceTint = md_theme_dark_surfaceTint,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)
