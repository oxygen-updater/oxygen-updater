package com.oxygenupdater.compose.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.google.accompanist.web.rememberSaveableWebViewState
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.Image
import com.oxygenupdater.compose.icons.LogoNotification
import com.oxygenupdater.compose.ui.CollapsingAppBar
import com.oxygenupdater.compose.ui.common.PullRefresh
import com.oxygenupdater.compose.ui.common.rememberCallback
import com.oxygenupdater.compose.ui.common.rememberTypedCallback
import com.oxygenupdater.compose.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.compose.ui.dialogs.defaultModalBottomSheetState
import com.oxygenupdater.compose.ui.news.ErrorSheet
import com.oxygenupdater.compose.ui.news.NewsItemScreen
import com.oxygenupdater.compose.ui.news.NewsItemViewModel
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.PROPERTY_LAST_NEWS_AD_SHOWN
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.viewmodels.BillingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Timer
import kotlin.concurrent.schedule

@OptIn(ExperimentalMaterial3Api::class)
class NewsItemActivity : ComposeSupportActionBarActivity(
    MainActivity.PAGE_NEWS,
    R.string.news
) {

    private val viewModel by viewModel<NewsItemViewModel>()
    private val billingViewModel by viewModel<BillingViewModel>()

    private var shouldDelayAdStart = false
    private var newsItemId = -1L

    private val fullScreenAdContentCallback = if (BuildConfig.DEBUG) object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() = logDebug(TAG, "Interstitial ad was dismissed")
        override fun onAdFailedToShowFullScreenContent(error: AdError) = logWarning(TAG, "Interstitial ad failed to show: $error")
        override fun onAdShowedFullScreenContent() = logDebug(TAG, "Interstitial ad was shown")
    } else null

    @Volatile
    private var bannerAdView: AdView? = null

    private val timer = Timer()

    private val interstitialAdLoadCallback = object : InterstitialAdLoadCallback() {
        override fun onAdFailedToLoad(error: LoadAdError) = logWarning(TAG, "Interstitial ad failed to load: $error")
        override fun onAdLoaded(ad: InterstitialAd) {
            logDebug(TAG, "Interstitial ad loaded")
            if (isFinishing) return

            if (BuildConfig.DEBUG) {
                ad.fullScreenContentCallback = fullScreenAdContentCallback
            }

            if (shouldDelayAdStart) {
                logDebug(TAG, "Showing interstitial ad in 5s")

                // Ensure only the latest task goes through
                timer.cancel()
                timer.schedule(5000) {
                    if (!isFinishing) ad.show(this@NewsItemActivity)
                }
            } else ad.show(this@NewsItemActivity)
        }
    }

    @Composable
    override fun scrollBehavior() = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    @Composable
    override fun TopAppBar() = CollapsingAppBar(scrollBehavior, { modifier ->
        val context = LocalContext.current
        val imageUrl = item?.imageUrl
        AsyncImage(
            imageUrl?.let {
                val density = LocalDensity.current
                remember(it, maxWidth) {
                    ImageRequest.Builder(context)
                        .data(it)
                        .size(density.run { Size(maxWidth.roundToPx(), 256.dp.roundToPx()) })
                        .build()
                }
            },
            stringResource(R.string.news), modifier,
            placeholder = rememberVectorPainter(CustomIcons.Image),
            error = rememberVectorPainter(CustomIcons.LogoNotification),
            contentScale = ContentScale.Crop,
            colorFilter = if (imageUrl == null) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null
        )
    }, item?.title ?: stringResource(R.string.loading), item?.authorName ?: stringResource(R.string.summary_please_wait)) {
        onBackPressed()
    }

    @Composable
    override fun Content() {
        // Ads should be shown if user hasn't bought the ad-free unlock
        val adFree by billingViewModel.hasPurchasedAdFree.collectAsStateWithLifecycle(
            !PrefManager.getBoolean(PrefManager.PROPERTY_AD_FREE, false)
        )
        val showAds = !adFree

        val state by viewModel.state.collectAsStateWithLifecycle()
        val webViewState = rememberSaveableWebViewState()
        val navigator = rememberWebViewNavigator()
        val onRefresh = remember(showAds, navigator) {
            {
                if (showAds) {
                    shouldDelayAdStart = true
                    setupInterstitialAd()
                }
                viewModel.refreshItem(newsItemId)
                navigator.reload()
            }
        }

        PullRefresh(state, { it?.isFullyLoaded != true }, onRefresh) {
            val sheetState = defaultModalBottomSheetState()
            var error by remember { mutableStateOf<String?>(null) }
            val hide = rememberCallback { error = null }
            BackHandler(sheetState.isVisible, hide)

            error?.let {
                ModalBottomSheet(hide, sheetState) {
                    ErrorSheet(it, hide, onRefresh)
                }
            }

            NewsItemScreen(state, scrollBehavior, webViewState, navigator, showAds, bannerAdInit = rememberTypedCallback {
                bannerAdView = it
            }, onError = rememberTypedCallback {
                error = it
            }, onLoadFinished = rememberTypedCallback {
                viewModel.markRead(it)
            })

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        if (!handleIntent(intent)) return onBackPressed()

        setupInterstitialAd()
        viewModel.refreshItem(newsItemId)
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

    private fun handleIntent(intent: Intent?) = when {
        intent == null -> false
        // This used to be a simple intent.extras != null check.
        // However, if the URL is clicked outside of the app, Android adds a "browser_id" extra,
        // which is set to the app's package name. So we need to explicitly check if the extras
        // we want (NEWS_ITEM_ID) or (DELAY_AD_START) are present.
        intent.hasExtra(INTENT_NEWS_ITEM_ID) || intent.hasExtra(INTENT_DELAY_AD_START) -> {
            shouldDelayAdStart = intent.getBooleanExtra(INTENT_DELAY_AD_START, false)
            newsItemId = intent.getLongExtra(INTENT_NEWS_ITEM_ID, -1L)
            newsItemId >= 0L
        }

        Intent.ACTION_VIEW == intent.action -> intent.data.let { data ->
            if (data == null) return@let false

            shouldDelayAdStart = true
            when (data.scheme) {
                "http", "https" -> {
                    val path = data.path ?: return@let false
                    val groupValues = LINK_PATH_REGEX.matchEntire(path)?.groupValues ?: return@let false
                    newsItemId = if (groupValues.size > 1) try {
                        groupValues[1].toLong()
                    } catch (e: NumberFormatException) {
                        -1L
                    } else return@let false
                    newsItemId >= 0L
                }
                // oxygenupdater://news/<id>
                "oxygenupdater" -> {
                    newsItemId = try {
                        data.lastPathSegment?.toLong() ?: -1L
                    } catch (e: NumberFormatException) {
                        -1L
                    }
                    newsItemId >= 0L
                }

                else -> false
            }
        }

        else -> false
    }

    /**
     * Interstitial ads are limited to only once per 5 minutes for better UX.
     *
     * v5.2.0 onwards, this frequency capping is configured within AdMob dashboard itself,
     * because it seemed to be more reliable than custom SharedPreferences-based handling
     * done prior to v5.2.0.
     *
     * @see [PROPERTY_LAST_NEWS_AD_SHOWN]
     */
    private fun setupInterstitialAd() = InterstitialAd.load(
        this,
        BuildConfig.AD_INTERSTITIAL_NEWS_ID,
        OxygenUpdater.buildAdRequest(),
        interstitialAdLoadCallback
    )

    companion object {
        private const val TAG = "NewsItemActivity"

        /**
         * Matches both the API link and new website links
         *
         * https://oxygenupdater.com/api/<version>/news-content/<id>/<lang>/<theme>
         * https://oxygenupdater.com/article/<id>/
         */
        private val LINK_PATH_REGEX = "^/(?:api/[v\\d.]+/news-content|article)/(\\d+)/?.*".toRegex(
            RegexOption.IGNORE_CASE
        )

        var item by mutableStateOf<NewsItem?>(null)
            internal set

        const val INTENT_NEWS_ITEM_ID = "NEWS_ITEM_ID"
        const val INTENT_DELAY_AD_START = "DELAY_AD_START"
    }
}
