package com.oxygenupdater.activities

import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.google.android.gms.ads.AdView
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.ui.common.loadBannerAd
import com.oxygenupdater.ui.theme.light

/**
 * Single responsibility: enable edge-to-edge.
 *
 * We're using [AppCompatActivity] instead of [androidx.activity.ComponentActivity] because of
 * [automatic per-app language](https://developer.android.com/guide/topics/resources/app-languages#androidx-impl)
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
abstract class BaseActivity : AppCompatActivity() {

    @Volatile
    protected var bannerAdView: AdView? = null

    protected fun onBannerAdInit(adView: AdView) {
        bannerAdView?.let {
            // Destroy previous AdView if it changed
            if (it != adView) it.destroy()
        }

        // Only one will be active at any time, so update reference
        bannerAdView = adView

        /** Load only if [setupMobileAds] has been called via [setupUmp] */
        if ((application as? OxygenUpdater)?.mobileAdsInitDone?.get() != true) return

        loadBannerAd(bannerAdView)
    }

    protected fun setupMobileAds() = (application as? OxygenUpdater)?.setupMobileAds {
        loadBannerAd(bannerAdView)
    }

    @Composable
    @ReadOnlyComposable
    protected fun EdgeToEdge() {
        val light = MaterialTheme.colorScheme.light
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { !light },
            navigationBarStyle = navigationBarStyle,
        )
    }

    companion object {
        private const val TAG = "BaseActivity"

        /**
         * Force even 3-button nav to be completely transparent on [Android 10+](https://github.com/android/nowinandroid/pull/817#issuecomment-1647079628)
         */
        private val navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    }
}
