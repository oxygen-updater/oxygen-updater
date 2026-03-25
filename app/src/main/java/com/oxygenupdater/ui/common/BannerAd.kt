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
    adWidth: Int, // dp
    modifier: Modifier = Modifier,
    adListener: AdListener? = null,
    onViewUpdate: (AdView) -> Unit,
) = if (LocalInspectionMode.current) {
    Text("AdView", Modifier.align(Alignment.CenterHorizontally))
} else AndroidView(
    factory = { context ->
        AdView(context).apply {
            setAdUnitId(adUnitId)
            /**
             * TODO(compose/ads): factory block doesn't execute again when [adWidth] changes,
             *  meaning [AdView.setAdSize] is never updated. Ideally we want it to, and also
             *  have the ad reloaded for an orientation change.
             */
            setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidth))

            adListener?.let { setAdListener(it) }
        }
    },
    update = onViewUpdate,
    modifier = modifier
)

/**
 * @param collapsibleBanner Default `false`. Controls whether we use Google's new collapsible
 *        banner ad format: https://developers.google.com/admob/android/banner/collapsible.
 *        **Note**: while collapsible banner ads have higher eCPMs, they have terrible UX,
 *        and so it shouldn't be enabled again unless Google improves on this front. Firstly,
 *        the button to collapse it is sometimes either very small, or in some cases it's not
 *        there at all (we've received screenshots over email). It also has a bug that has
 *        existed at least since https://github.com/oxygen-updater/oxygen-updater/commit/79a7eb9f0cc4969bfad4234f8681c6fd1c922425.
 *        Back then (Aug 16, 2024), this format was marked 'experimental', but this bug still
 *        exists even though it's not experimental anymore. By memory, the culprit ads usually
 *        were TikTok or Temu that were full-height without collapsible buttons. Not sure.
 *        This entire doc-comment is kept so that the next time we feel tempted to turn it on
 *        again, we're reminded of everything wrong with the format. (especially user experience!)
 */
inline fun buildAdRequest(
    collapsibleBanner: Boolean = false,
) = AdRequest.Builder().apply {
    // https://developers.google.com/admob/android/banner/collapsible
    if (collapsibleBanner) addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply {
        putString("collapsible", "bottom")
    })
}.build()

fun loadBannerAd(adView: AdView?) = adView?.loadAd(buildAdRequest())?.also {
    logDebug(TAG, "loading")
} ?: logDebug(TAG, "adView = null")

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
