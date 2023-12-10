package com.oxygenupdater.activities

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdView
import com.oxygenupdater.R
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.faq.FaqScreen
import com.oxygenupdater.ui.faq.FaqViewModel
import com.oxygenupdater.viewmodels.BillingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FaqActivity : SupportActionBarActivity(
    MainActivity.PageAbout,
    R.string.faq,
) {

    private val viewModel: FaqViewModel by viewModels()
    private val billingViewModel: BillingViewModel by viewModels()

    @Volatile
    private var bannerAdView: AdView? = null

    @Composable
    override fun Content(modifier: Modifier) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val showAds by billingViewModel.shouldShowAds.collectAsStateWithLifecycle()

        PullRefresh(
            state = state,
            shouldShowProgressIndicator = { it.isEmpty() },
            onRefresh = viewModel::refresh,
        ) {
            FaqScreen(
                state = state,
                showAds = showAds,
                onBannerAdInit = { bannerAdView = it },
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
