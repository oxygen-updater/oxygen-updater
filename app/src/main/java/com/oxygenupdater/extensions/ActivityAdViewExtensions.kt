package com.oxygenupdater.extensions

import android.app.Activity
import android.util.DisplayMetrics
import android.widget.FrameLayout
import androidx.annotation.StringRes
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Creates a full-width anchored adaptive banner ad within the [container],
 * which replaces the deprecated [AdSize.SMART_BANNER] (used in 4.0.0 — 5.3.0).
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun Activity.fullWidthAnchoredAdaptiveBannerAd(
    @StringRes adUnitIdStr: Int,
    container: FrameLayout
) = AdView(this).apply {
    adUnitId = getString(adUnitIdStr)
    adSize = fullWidthAdSize(container)
}.also {
    container.addView(it)
}

/**
 * Determine the screen width (less decorations) to use for the ad width.
 * If the ad hasn't been laid out, default to the full screen width.
 */
private fun Activity.fullWidthAdSize(
    container: FrameLayout
) = DisplayMetrics().run {
    windowManager.defaultDisplay.getMetrics(this)

    val adWidthPx = container.width.let {
        if (it == 0) widthPixels else it
    }.toFloat()

    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
        this@fullWidthAdSize,
        (adWidthPx / density).toInt()
    )
}
