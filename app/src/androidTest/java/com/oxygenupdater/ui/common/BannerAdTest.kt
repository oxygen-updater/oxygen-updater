package com.oxygenupdater.ui.common

import androidx.compose.foundation.layout.Column
import com.google.android.gms.ads.AdView
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.ComposeBaseTest
import org.junit.Test

class BannerAdTest : ComposeBaseTest() {

    private lateinit var bannerAdView: AdView

    @Test
    fun bannerAd_view() {
        setContent {
            Column {
                BannerAd(
                    adUnitId = BuildConfig.AD_BANNER_MAIN_ID,
                    adWidth = 320,
                    onViewUpdate = { bannerAdView = it },
                )
            }
        }

        assert(::bannerAdView.isInitialized) { "AdView not initialized" }
        assert(bannerAdView.adSize != null) { "Ad size must not be null" }
        assert(bannerAdView.adUnitId == BuildConfig.AD_BANNER_MAIN_ID) {
            "Ad unit ID did not match. Expected: ${BuildConfig.AD_BANNER_MAIN_ID}, actual: ${bannerAdView.adUnitId}."
        }
    }
}
