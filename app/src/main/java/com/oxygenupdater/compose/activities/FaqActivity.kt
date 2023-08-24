package com.oxygenupdater.compose.activities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdView
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.common.PullRefresh
import com.oxygenupdater.compose.ui.common.rememberCallback
import com.oxygenupdater.compose.ui.common.rememberTypedCallback
import com.oxygenupdater.compose.ui.faq.FaqScreen
import com.oxygenupdater.compose.ui.faq.FaqViewModel
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.viewmodels.BillingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class FaqActivity : ComposeSupportActionBarActivity(
    MainActivity.PAGE_ABOUT,
    R.string.faq,
) {

    private val viewModel by viewModel<FaqViewModel>()
    private val billingViewModel by viewModel<BillingViewModel>()

    @Volatile
    private var bannerAdView: AdView? = null

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsStateWithLifecycle()

        // Ads should be shown if user hasn't bought the ad-free unlock
        val showAds = !billingViewModel.hasPurchasedAdFree.collectAsStateWithLifecycle(
            PrefManager.getBoolean(PrefManager.PROPERTY_AD_FREE, false)
        ).value

        PullRefresh(state, rememberTypedCallback { it.isEmpty() }, rememberCallback(viewModel::refresh)) {
            FaqScreen(state, showAds, rememberTypedCallback {
                bannerAdView = it
            })
        }
    }

    override fun onResume() = super.onResume().also {
        bannerAdView?.resume()
    }

    override fun onPause() = super.onPause().also {
        bannerAdView?.pause()
    }

    override fun onDestroy() = super.onDestroy().also {
        bannerAdView?.destroy()
    }
}
