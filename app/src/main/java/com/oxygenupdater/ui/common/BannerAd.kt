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
import com.oxygenupdater.utils.logDebug

/**
 * @param adListener act on ad callbacks, usually via [adLoadListener]
 */
@Composable
fun ColumnScope.BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
    adListener: AdListener? = null,
    viewUpdated: (AdView) -> Unit,
) = if (LocalInspectionMode.current) {
    Text("AdView", Modifier.align(Alignment.CenterHorizontally))
} else AndroidView(
    factory = { context ->
        AdView(context).apply {
            setAdUnitId(adUnitId)
            // TODO(compose/bug): size doesn't adjust to screen changes
            // TODO(compose/bug): FULL_WIDTH is not suitable for use with NavType.SideRail, as that takes up some of the screen's width
            setAdSize(AdSize(AdSize.FULL_WIDTH, AdSize.AUTO_HEIGHT))

            loadAd(OxygenUpdater.buildAdRequest())

            adListener?.let { setAdListener(it) }
        }
    },
    update = viewUpdated,
    modifier = modifier
)

fun adLoadListener(callback: (loaded: Boolean) -> Unit) = object : AdListener() {
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
