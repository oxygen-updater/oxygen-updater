package com.oxygenupdater.compose.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

private val LightForeground = Color(0xFF212121)
private val DarkBackground = Color(0xFF121212)
private val LightBackgroundVariant = Color(0xFFF5F5F5)
private val DarkBackgroundVariant = Color(0xFF1D1D1D)
private val LightPositive = Color(0xFF4CAF50)
private val DarkPositive = Color(0xFF81C784)
private val LightWarn = Color(0xFFFF9800)
private val DarkWarn = Color(0xFFFFb74d)

/** `isLight` is M2-only, so we're using this to reduce M2 specificity */
val Colors.light get() = surface == Color.White

val Colors.backgroundVariant get() = if (light) LightBackgroundVariant else DarkBackgroundVariant
val Colors.positive get() = if (light) LightPositive else DarkPositive
val Colors.warn get() = if (light) LightWarn else DarkWarn

val LightColors = lightColors(
    primary = Color(0xFFF50514),
    primaryVariant = Color(0xFFD70005),
    secondary = Color(0xFF00C5E6),
    secondaryVariant = Color(0xFF009EB7),
    background = Color.White,
    surface = Color.White,
    error = Color(0xFFF44336),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightForeground,
    onSurface = LightForeground,
    onError = LightForeground,
)

val DarkColors = darkColors(
    primary = Color(0xFFFF3D39),
    primaryVariant = Color(0xFFF50514),
    secondary = Color(0xFF4FD6F0),
    secondaryVariant = Color(0xFF00C5E6),
    background = DarkBackground,
    surface = DarkBackground,
    error = Color(0xFFEF5350),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = LightForeground,
)

/*
 * Material 3 Colors
 *
 * GENERATOR: https://m3.material.io/theme-builder#/custom
 *    COLORS: https://forums-images.oneplus.net/attachments/1260/1260743-f51b09eec9d7a879a914042a8aae8a89.jpg (https://community.oneplus.com/thread/1201086)
 *
 *   Primary: #f50514 (red)
 * Secondary: #00c5e6 (teal)
 *  Tertiary: #f2f0fa (lavender)
 *   Neutral: #ffffff (white)
 */
