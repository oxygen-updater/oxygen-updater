package com.oxygenupdater.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        setupMobileAds()
        super.onCreate(savedInstanceState)
    }

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
                onBannerAdInit = ::onBannerAdInit,
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
