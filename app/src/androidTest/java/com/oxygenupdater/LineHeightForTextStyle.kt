package com.oxygenupdater

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Keep in sync with [com.oxygenupdater.ui.theme.appTypography] */
@JvmInline
value class LineHeightForTextStyle private constructor(val value: TextUnit) {

    override fun toString() = "LineHeightForTextStyle." + when (this) {
        displayLarge -> "displayLarge"
        displayMedium -> "displayMedium"
        displaySmall -> "displaySmall"
        headlineLarge -> "headlineLarge"
        headlineMedium -> "headlineMedium"
        headlineSmall -> "headlineSmall"
        titleLarge -> "titleLarge"
        titleMedium -> "titleMedium"
        titleSmall -> "titleSmall"
        bodyLarge -> "bodyLarge"
        bodyMedium -> "bodyMedium"
        bodySmall -> "bodySmall"
        labelLarge -> "labelLarge"
        labelMedium -> "labelMedium"
        labelSmall -> "labelSmall"
        else -> "Invalid"
    }

    companion object {
        val displayLarge = LineHeightForTextStyle(64.sp)
        val displayMedium = LineHeightForTextStyle(52.sp)
        val displaySmall = LineHeightForTextStyle(44.sp)
        val headlineLarge = LineHeightForTextStyle(40.sp)
        val headlineMedium = LineHeightForTextStyle(36.sp)
        val headlineSmall = LineHeightForTextStyle(32.sp)
        val titleLarge = LineHeightForTextStyle(28.sp)
        val titleMedium = LineHeightForTextStyle(24.sp)
        val titleSmall = LineHeightForTextStyle(20.sp)
        val bodyLarge = LineHeightForTextStyle(24.sp)
        val bodyMedium = LineHeightForTextStyle(20.sp)
        val bodySmall = LineHeightForTextStyle(16.sp)
        val labelLarge = LineHeightForTextStyle(20.sp)
        val labelMedium = LineHeightForTextStyle(16.sp)
        val labelSmall = LineHeightForTextStyle(16.sp)
    }
}
