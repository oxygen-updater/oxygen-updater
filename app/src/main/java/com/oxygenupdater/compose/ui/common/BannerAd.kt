package com.oxygenupdater.compose.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
 * @param adLoaded if supplied, [Modifier.navigationBarsPadding] will be applied on the
 * assumption that the only view placed below this is the navigation bar.
 */
@Composable
fun ColumnScope.BannerAd(
    adUnitId: String,
    adLoaded: MutableState<Boolean>? = null,
    view: (AdView) -> Unit,
) = if (LocalInspectionMode.current) {
    Text("AdView", Modifier.align(Alignment.CenterHorizontally))
} else AndroidView(factory = {
    AdView(it).apply {
        setAdUnitId(adUnitId)
        setAdSize(AdSize(AdSize.FULL_WIDTH, AdSize.AUTO_HEIGHT))

        loadAd(OxygenUpdater.buildAdRequest())

        if (adLoaded != null) adListener = object : AdListener() {
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
}, if (adLoaded?.value == true) Modifier.navigationBarsPadding() else Modifier, view)

private const val TAG = "BannerAd"
