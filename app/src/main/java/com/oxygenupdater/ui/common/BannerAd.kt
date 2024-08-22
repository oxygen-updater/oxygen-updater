package com.oxygenupdater.ui.common

import android.os.Bundle
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.oxygenupdater.utils.logDebug
import java.util.UUID

/**
 * Lays out an [AdView]. Note that [AdView.loadAd] is not called in the factory
 * block here. Instead, call it via the reference received from [onViewUpdate].
 *
 * @param adListener act on ad callbacks, usually via [adLoadListener]
 *
 * @see com.oxygenupdater.activities.MainActivity.onBannerAdInit
 */
@Composable
fun ColumnScope.BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
    adListener: AdListener? = null,
    onViewUpdate: (AdView) -> Unit,
) = if (LocalInspectionMode.current) {
    Text("AdView", Modifier.align(Alignment.CenterHorizontally))
} else AndroidView(
    factory = { context ->
        AdView(context).apply {
            setAdUnitId(adUnitId)
            // TODO(compose/bug): size doesn't adjust to screen changes
            // TODO(compose/bug): FULL_WIDTH is not suitable for use with NavType.SideRail, as that takes up some of the screen's width
            setAdSize(AdSize(AdSize.FULL_WIDTH, AdSize.AUTO_HEIGHT))

            adListener?.let { setAdListener(it) }
        }
    },
    update = onViewUpdate,
    modifier = modifier
)

inline fun buildAdRequest(
    additionalConfig: AdRequest.Builder.() -> Unit = {},
) = AdRequest.Builder().apply { additionalConfig() }.build()

fun loadBannerAd(adView: AdView?) = adView?.loadAd(buildAdRequest {
    // https://developers.google.com/admob/android/banner/collapsible
    addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply {
        putString("collapsible", "bottom")
        // Prevent subsequent loads to also be collapsible (bad UX)
        putString("collapsible_request_id", UUID.randomUUID().toString())
    })
})?.also { logDebug(TAG, "loading") } ?: logDebug(TAG, "adView = null")

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
