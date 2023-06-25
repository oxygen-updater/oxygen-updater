package com.oxygenupdater.compose.ui.theme

import android.content.Context
import android.content.res.Resources
import androidx.annotation.FontRes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.oxygenupdater.R

/**
 * [Typography] automatically falls back to default font family if null
 */
private fun Context.tryFont(
    @FontRes fontResId: Int,
) = (try {
    ResourcesCompat.getFont(this, fontResId)
} catch (e: Resources.NotFoundException) {
    null
})?.let {
    FontFamily(it)
}

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Composable
fun appTypography() = with(LocalContext.current) {
    val googleSans = tryFont(R.font.google_sans)
    val googleSansMedium = tryFont(R.font.google_sans_medium)

    // https://material.io/design/typography/the-type-system.html#type-scale
    Typography(
        h1 = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Light,
            fontSize = 96.sp,
            letterSpacing = (-1.5).sp,
        ),
        h2 = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Light,
            fontSize = 60.sp,
            letterSpacing = (-0.5).sp,
        ),
        h3 = DefaultTextStyle.copy(
            fontFamily = googleSansMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 48.sp,
            letterSpacing = 0.sp,
        ),
        h4 = DefaultTextStyle.copy(
            fontFamily = googleSansMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 34.sp,
            letterSpacing = 0.25.sp,
        ),
        h5 = DefaultTextStyle.copy(
            fontFamily = googleSansMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp, //  keep in sync with CollapsingAppBarTitleSize in ../AppBar.kt
            letterSpacing = 0.sp,
        ),
        h6 = DefaultTextStyle.copy(
            fontFamily = googleSansMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp, //  keep in sync with CollapsingAppBarTitleSize in ../AppBar.kt
            letterSpacing = 0.15.sp,
        ),
        subtitle1 = DefaultTextStyle.copy(
            fontFamily = googleSansMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            letterSpacing = 0.15.sp,
            lineHeight = 24.sp,
        ),
        subtitle2 = DefaultTextStyle.copy(
            fontFamily = googleSansMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 0.1.sp,
            lineHeight = 20.sp,
        ),
        body1 = DefaultTextStyle.copy(
            fontSize = 16.sp, //  keep in sync with CollapsingAppBarSubtitleSize in ../AppBar.kt
            letterSpacing = 0.5.sp,
            lineHeight = 24.sp,
        ),
        body2 = DefaultTextStyle.copy(
            fontSize = 14.sp, //  keep in sync with CollapsingAppBarSubtitleSize in ../AppBar.kt
            letterSpacing = 0.25.sp,
            lineHeight = 20.sp,
        ),
        button = DefaultTextStyle.copy(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 1.25.sp,
            lineHeight = 20.sp,
        ),
        caption = DefaultTextStyle.copy(
            fontSize = 12.sp,
            letterSpacing = 0.4.sp,
            lineHeight = 18.sp,
        ),
        overline = DefaultTextStyle.copy(
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            lineHeight = 16.sp,
        )
    )
}

val DefaultTextStyle = TextStyle.Default.copy(
    platformStyle = PlatformTextStyle(
        includeFontPadding = false
    ),
    lineHeightStyle = LineHeightStyle(
        LineHeightStyle.Alignment.Center,
        LineHeightStyle.Trim.None
    ),
)
