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
import com.oxygenupdater.ui.install.InstallGuideScreen
import com.oxygenupdater.ui.install.InstallGuideViewModel
import com.oxygenupdater.viewmodels.BillingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class InstallGuideActivity : SupportActionBarActivity(
    MainActivity.PageUpdate,
    R.string.install_guide,
) {

    private val viewModel by viewModel<InstallGuideViewModel>()
    private val billingViewModel by viewModel<BillingViewModel>()

    private val showDownloadInstructions = intent.let {
        it == null || it.getBooleanExtra(IntentShowDownloadInstructions, false)
    }

    @Volatile
    private var bannerAdView: AdView? = null

    @Composable
    override fun Content(modifier: Modifier) {
        val state by viewModel.state.collectAsStateWithLifecycle()

        // Ads should be shown if user hasn't bought the ad-free unlock
        val showAds = !billingViewModel.hasPurchasedAdFree.collectAsStateWithLifecycle(
            PrefManager.getBoolean(PrefManager.KeyAdFree, false)
        ).value

        PullRefresh(state, { it.isEmpty() }, viewModel::refresh) {
            InstallGuideScreen(modifier, state, showDownloadInstructions, showAds, rememberTypedCallback {
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

    companion object {
        const val IntentShowDownloadInstructions = "show_download_instructions"
    }
}
