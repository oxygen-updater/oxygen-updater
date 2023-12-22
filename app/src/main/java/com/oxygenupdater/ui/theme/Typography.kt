package com.oxygenupdater.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.oxygenupdater.R

/** Note: update [com.oxygenupdater.LineHeightForStyle] if line heights are changed */
fun appTypography(): Typography {
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

    val googleSans = try {
        val font = GoogleFont("Google Sans")
        FontFamily(
            Font(googleFont = font, fontProvider = provider),
            Font(googleFont = font, fontProvider = provider, weight = FontWeight.Medium)
        )
    } catch (e: Exception) {
        FontFamily.Default
    }

    // https://developer.android.com/jetpack/compose/designsystems/material3#typography
    return Typography(
        displayLarge = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
        ),
        displayMedium = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        ),
        headlineLarge = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp, //  keep in sync with CollapsingAppBarTitleSize in ../AppBar.kt
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
        ),
        titleLarge = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp, //  keep in sync with CollapsingAppBarTitleSize in ../AppBar.kt
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp,
        ),
        titleSmall = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = DefaultTextStyle.copy(
            fontSize = 16.sp, //  keep in sync with CollapsingAppBarSubtitleSize in ../AppBar.kt
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = DefaultTextStyle.copy(
            fontSize = 14.sp, //  keep in sync with CollapsingAppBarSubtitleSize in ../AppBar.kt
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = DefaultTextStyle.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = DefaultTextStyle.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = DefaultTextStyle.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}

val DefaultTextStyle = TextStyle.Default.copy(
    fontFamily = FontFamily.Default,
    // These are defaults in M2 v1.6 & M3 v1.2 onwards
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)
