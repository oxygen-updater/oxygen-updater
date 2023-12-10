package com.oxygenupdater.activities

import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Image
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.ui.CollapsingAppBar
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.dialogs.ArticleErrorSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.news.NewsItemScreen
import com.oxygenupdater.ui.news.NewsItemViewModel
import com.oxygenupdater.utils.logDebug
import com.oxygenupdater.utils.logWarning
import com.oxygenupdater.viewmodels.BillingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Timer
import kotlin.concurrent.schedule

@OptIn(ExperimentalMaterial3Api::class)
class NewsItemActivity : SupportActionBarActivity(
    MainActivity.PageNews,
    R.string.news
) {

    private val viewModel by viewModel<NewsItemViewModel>()
    private val billingViewModel by viewModel<BillingViewModel>()

    private var shouldDelayAdStart = false
    private var newsItemId = NotSetL

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

                timer.cancel() // ensure only the latest task goes through
                timer.schedule(5000) {
                    if (!isFinishing) ad.show(this@NewsItemActivity)
                }
            } else ad.show(this@NewsItemActivity)
        }
    }

    @Composable
    override fun scrollBehavior() = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    @Composable
    override fun TopAppBar() = CollapsingAppBar(
        scrollBehavior = scrollBehavior,
        image = { modifier ->
            val context = LocalContext.current
            val imageUrl = item?.imageUrl
            AsyncImage(
                model = imageUrl?.let {
                    val density = LocalDensity.current
                    remember(it, maxWidth) {
                        ImageRequest.Builder(context)
                            .data(it)
                            .size(density.run { Size(maxWidth.roundToPx(), 256.dp.roundToPx()) })
                            .build()
                    }
                },
                contentDescription = stringResource(R.string.news),
                placeholder = rememberVectorPainter(CustomIcons.Image),
                error = rememberVectorPainter(CustomIcons.LogoNotification),
                contentScale = ContentScale.Crop,
                colorFilter = if (imageUrl == null) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                modifier = modifier
            )
        },
        title = item?.title ?: stringResource(R.string.loading),
        subtitle = item?.authorName ?: stringResource(R.string.summary_please_wait),
        onNavIconClick = ::onBackPressed,
    )

    @Suppress("DEPRECATION")
    @Composable
    override fun Content(modifier: Modifier) {
        // Ads should be shown if user hasn't bought the ad-free unlock
        val showAds = !billingViewModel.hasPurchasedAdFree.collectAsStateWithLifecycle(
            PrefManager.getBoolean(PrefManager.KeyAdFree, false)
        ).value

        LaunchedEffect(showAds) { if (showAds) setupInterstitialAd() }

        val state by viewModel.state.collectAsStateWithLifecycle()
        val webViewState = rememberSaveableWebViewState()
        val navigator = rememberWebViewNavigator()
        val onRefresh = {
            if (showAds) {
                shouldDelayAdStart = true
                setupInterstitialAd()
            }
            viewModel.refreshItem(newsItemId)
            navigator.reload()
        }

        PullRefresh(
            state = state,
            shouldShowProgressIndicator = { it?.isFullyLoaded != true },
            onRefresh = onRefresh,
        ) {
            var errorTitle by remember { mutableStateOf<String?>(null) }
            errorTitle?.let { title ->
                ModalBottomSheet({ errorTitle = null }) { ArticleErrorSheet(it, title, confirm = onRefresh) }
            }

            NewsItemScreen(
                state = state,
                webViewState = webViewState,
                navigator = navigator,
                showAds = showAds,
                bannerAdInit = { bannerAdView = it },
                onError = { errorTitle = it },
                onLoadFinished = viewModel::markRead,
                modifier = modifier
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        if (!handleIntent(intent)) return onBackPressed()

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
        intent.hasExtra(IntentNewsItemId) || intent.hasExtra(IntentDelayAdStart) -> {
            shouldDelayAdStart = intent.getBooleanExtra(IntentDelayAdStart, false)
            newsItemId = intent.getLongExtra(IntentNewsItemId, NotSetL)
            newsItemId >= 0L
        }

        Intent.ACTION_VIEW == intent.action -> intent.data.let { data ->
            if (data == null) return@let false

            shouldDelayAdStart = true
            when (data.scheme) {
                "http", "https" -> {
                    val path = data.path ?: return@let false
                    val groupValues = LinkPathRegex.matchEntire(path)?.groupValues ?: return@let false
                    newsItemId = if (groupValues.size > 1) try {
                        groupValues[1].toLong()
                    } catch (e: NumberFormatException) {
                        NotSetL
                    } else return@let false
                    newsItemId >= 0L
                }
                // oxygenupdater://news/<id>
                "oxygenupdater" -> {
                    newsItemId = try {
                        data.lastPathSegment?.toLong() ?: NotSetL
                    } catch (e: NumberFormatException) {
                        NotSetL
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
        private val LinkPathRegex = "^/(?:api/[v\\d.]+/news-content|article)/(\\d+)/?.*".toRegex(
            RegexOption.IGNORE_CASE
        )

        var item by mutableStateOf<NewsItem?>(null)
            internal set

        const val IntentNewsItemId = "NEWS_ITEM_ID"
        const val IntentDelayAdStart = "DELAY_AD_START"
    }
}
