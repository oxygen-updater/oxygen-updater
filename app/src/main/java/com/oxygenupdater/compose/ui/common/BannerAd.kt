package com.oxygenupdater.compose.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.utils.Logger.logDebug

@Composable
fun ColumnScope.BannerAd(
    adUnitId: String,
    adLoaded: MutableState<Boolean>,
    view: (AdView) -> Unit,
) = if (LocalInspectionMode.current) {
    Text("AdView", Modifier.align(Alignment.CenterHorizontally))
} else LocalConfiguration.current.screenWidthDp.let { screenWidthDp ->
    AndroidView(factory = {
        AdView(it).apply {
            setAdUnitId(adUnitId)
            setAdSize(getCurrentOrientationAnchoredAdaptiveBannerAdSize(it, screenWidthDp))

            loadAd(OxygenUpdater.buildAdRequest())

            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    logDebug(TAG, "Banner ad failed to load: $error")
                    adLoaded.value = false
                }

                override fun onAdLoaded() {
                    logDebug(TAG, "Banner ad loaded")
                    adLoaded.value = true
                }
            }
        }
        // We draw the activity edge-to-edge, so nav bar padding should be applied only if ad loaded
    }, if (adLoaded.value) Modifier.navigationBarsPadding() else Modifier, view)
}

private const val TAG = "BannerAd"
