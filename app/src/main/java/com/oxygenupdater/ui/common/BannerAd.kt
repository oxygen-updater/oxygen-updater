package com.oxygenupdater.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.utils.Logger.logDebug

/**
 * @param adListener act on ad callbacks, usually via [adLoadListener]
 */
@Composable
fun ColumnScope.BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
    adListener: AdListener? = null,
    view: (AdView) -> Unit,
) = if (LocalInspectionMode.current) {
    Text("AdView", Modifier.align(Alignment.CenterHorizontally))
} else AndroidView(factory = { context ->
    AdView(context).apply {
        setAdUnitId(adUnitId)
        setAdSize(AdSize(AdSize.FULL_WIDTH, AdSize.AUTO_HEIGHT))

        loadAd(OxygenUpdater.buildAdRequest())

        adListener?.let { setAdListener(it) }
    }
}, modifier, view)

fun adLoadListener(callback: (Boolean) -> Unit) = object : AdListener() {
    override fun onAdFailedToLoad(error: LoadAdError) {
        logDebug(TAG, "Banner ad failed to load: $error")
        callback(false)
    }

    override fun onAdLoaded() {
        logDebug(TAG, "Banner ad loaded")
        callback(true)
    }
}

private const val TAG = "BannerAd"
