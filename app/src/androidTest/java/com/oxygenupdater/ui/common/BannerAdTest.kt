package com.oxygenupdater.ui.common

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.view.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
                    adWidth = 320.dp,
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
