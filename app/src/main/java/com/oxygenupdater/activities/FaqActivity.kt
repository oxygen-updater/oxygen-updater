package com.oxygenupdater.activities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdView
import com.oxygenupdater.R
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.rememberTypedCallback
import com.oxygenupdater.ui.faq.FaqScreen
import com.oxygenupdater.ui.faq.FaqViewModel
import com.oxygenupdater.viewmodels.BillingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class FaqActivity : SupportActionBarActivity(
    MainActivity.PageAbout,
    R.string.faq,
) {

    private val viewModel by viewModel<FaqViewModel>()
    private val billingViewModel by viewModel<BillingViewModel>()

    @Volatile
    private var bannerAdView: AdView? = null

    @Composable
    override fun Content(modifier: Modifier) {
        val state by viewModel.state.collectAsStateWithLifecycle()

        // Ads should be shown if user hasn't bought the ad-free unlock
        val showAds = !billingViewModel.hasPurchasedAdFree.collectAsStateWithLifecycle(
            PrefManager.getBoolean(PrefManager.KeyAdFree, false)
        ).value

        PullRefresh(
            state = state,
            shouldShowProgressIndicator = { it.isEmpty() },
            onRefresh = viewModel::refresh,
        ) {
            FaqScreen(
                state = state,
                showAds = showAds,
                bannerAdInit = rememberTypedCallback { bannerAdView = it },
                modifier = modifier
            )
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
