package com.oxygenupdater.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_SHOW_DATE
import android.text.format.DateUtils.FORMAT_SHOW_TIME
import android.text.format.DateUtils.FORMAT_SHOW_YEAR
import android.webkit.WebViewClient.ERROR_BAD_URL
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import coil.load
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.OxygenUpdater.Companion.buildAdRequest
import com.oxygenupdater.R
import com.oxygenupdater.adapters.NewsItemButtonAdapter
import com.oxygenupdater.adapters.NewsListAdapter
import com.oxygenupdater.databinding.ActivityNewsItemBinding
import com.oxygenupdater.dialogs.Dialogs
import com.oxygenupdater.exceptions.NetworkException
import com.oxygenupdater.extensions.fullWidthAnchoredAdaptiveBannerAd
import com.oxygenupdater.internal.WebViewClient
import com.oxygenupdater.internal.WebViewError
import com.oxygenupdater.internal.settings.PrefManager.PROPERTY_LAST_NEWS_AD_SHOWN
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.ServerPostResult
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.ThemeUtils
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.viewmodels.NewsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDateTime

class NewsItemActivity : SupportActionBarActivity(
    R.layout.activity_news_item,
    MainActivity.PAGE_NEWS
) {

    private lateinit var bannerAdView: AdView

    private val newsViewModel by viewModel<NewsViewModel>()

    private var shouldDelayAdStart = false
    private var hasReadArticle = false
    private var newsItemId = -1L

    private var fullUrl = ""

    /**
     * Re-use the same observer to avoid duplicated callbacks
     */
    private val markNewsItemReadObserver = Observer<ServerPostResult?> { result ->
        if (result?.success == false) {
            logError(
                "NewsActivity",
                NetworkException("Error marking news item as read on the server: ${result.errorMessage}")
            )
        }
    }

    private val fullScreenAdContentCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() = logDebug(
            TAG,
            "Interstitial ad was dismissed"
        )

        override fun onAdFailedToShowFullScreenContent(
            adError: AdError,
        ) = logWarning(
            TAG,
            "Interstitial ad failed to show: $adError"
        )

        override fun onAdShowedFullScreenContent() = logDebug(
            TAG,
            "Interstitial ad was shown"
        )
    }

    private val interstitialAdLoadCallback = object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(
            ad: InterstitialAd,
        ) = logDebug(TAG, "Interstitial ad loaded").also {
            if (!isFinishing) {
                ad.fullScreenContentCallback = fullScreenAdContentCallback

                if (shouldDelayAdStart) {
                    logDebug(TAG, "Showing interstitial ad in 5s")

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!isFinishing) {
                            ad.show(this@NewsItemActivity)
                        }
                    }, 5000)
                } else {
                    ad.show(this@NewsItemActivity)
                }
            }
        }

        override fun onAdFailedToLoad(
            loadAdError: LoadAdError,
        ) = logWarning(
            TAG,
            "Interstitial ad failed to load: $loadAdError"
        )
    }

    /**
     * Re-use the same observer to avoid duplicated callbacks.
     *
     * Note: JS is required to load videos and other dynamic content
     */
    @SuppressLint("SetJavaScriptEnabled")
    private val fetchNewsItemObserver = Observer<NewsItem?> { newsItem ->
        if (newsItem == null || !newsItem.isFullyLoaded) {
            showErrorState()

            return@Observer
        }

        // Display the title of the article.
        binding.collapsingToolbarLayout.title = newsItem.title
        // Display the name of the author of the article
        binding.collapsingToolbarLayout.subtitle = newsItem.authorName

        binding.collapsingToolbarImage.load(newsItem.imageUrl) {
            placeholder(R.drawable.image)
            error(R.drawable.logo_notification)
        }

        binding.buttonRecyclerView.let { recyclerView ->
            val verticalDecorator = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
            val horizontalDecorator = DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL)

            ContextCompat.getDrawable(this, R.drawable.divider)?.let {
                verticalDecorator.setDrawable(it)
                horizontalDecorator.setDrawable(it)

                recyclerView.addItemDecoration(verticalDecorator)
                recyclerView.addItemDecoration(horizontalDecorator)
            }

            // Performance optimization
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = NewsItemButtonAdapter(this, newsItem)
        }

        // Display the subtitle of the article.
        binding.newsItemSubtitle.apply {
            newsItem.subtitle.let {
                isVisible = it != null
                text = it
            }
        }

        // Display the last update time of the article.
        binding.newsDatePublished.apply {
            val dateTimePrefix = if (newsItem.dateLastEdited == null && newsItem.datePublished != null) {
                newsItem.datePublished
            } else {
                newsItem.dateLastEdited
            }?.let {
                val userDateTime = LocalDateTime.parse(it.replace(" ", "T"))
                    .atZone(Utils.SERVER_TIME_ZONE)
                    .toInstant().toEpochMilli()

                // Weird bug in `android.text.format` that causes a crash in
                // older Android versions, due to using the [FORMAT_SHOW_TIME]
                // flag. Hacky workaround is to call the function again, but
                // without this flag. See https://stackoverflow.com/a/52665211.
                text = try {
                    DateUtils.getRelativeTimeSpanString(
                        userDateTime,
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS,
                        FORMAT_SHOW_TIME or FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR
                    )
                } catch (e: NullPointerException) {
                    DateUtils.getRelativeTimeSpanString(
                        userDateTime,
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS,
                        FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR
                    )
                }
            }

            isVisible = dateTimePrefix != null
        }

        // Display the contents of the article.
        binding.webView.apply {
            // must be done to avoid the white background in dark themes
            setBackgroundColor(Color.TRANSPARENT)

            isVisible = true
            settings.javaScriptEnabled = true
            settings.userAgentString = OxygenUpdater.APP_USER_AGENT

            if (newsItem.text.isNullOrEmpty()) {
                loadDataWithBaseURL("", getString(R.string.news_empty), "text/html", "UTF-8", "")
            } else {
                // since we can't edit CSS in WebViews,
                // append 'Light' or 'Dark' to newsContentUrl to get the corresponding themed version
                // backend handles CSS according to material spec
                fullUrl = newsItem.apiUrl + if (ThemeUtils.isNightModeActive(context)) "Dark" else "Light"

                loadUrl(fullUrl)
            }

            // disable loading state once page is completely loaded
            webViewClient = WebViewClient(context) { error ->
                hideLoadingState()

                if (error != null) {
                    showErrorState(error)
                }
            }
        }

        hasReadArticle = true
        // Mark the item as read on the server (to increase times read counter)
        newsViewModel.markNewsItemRead(newsItem).observe(this, markNewsItemReadObserver)
    }

    private lateinit var binding: ActivityNewsItemBinding
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) = super.onCreate(savedInstanceState).also {
        binding = ActivityNewsItemBinding.bind(rootView)
        bannerAdView = fullWidthAnchoredAdaptiveBannerAd(
            BuildConfig.AD_BANNER_NEWS_ID,
            binding.newsArticleAdViewContainer
        )

        if (!handleIntent(intent)) {
            onBackPressed()
            return
        }

        setupAds()
        loadNewsItem()
    }

    override fun onResume() = super.onResume().also {
        bannerAdView.resume()
        binding.webView.onResume()
    }

    override fun onPause() = super.onPause().also {
        bannerAdView.pause()
        binding.webView.onPause()
    }

    override fun onDestroy() = super.onDestroy().also {
        bannerAdView.destroy()
        // We're invoking the listener here so that RecyclerView's standard item change/remove
        // animations run just after leaving the activity. This has two advantages:
        // 1. Better for UX, since the user can see the animations play out, which may
        //    serve as a hint that read status has changed. Change animations (opacity fade)
        //    run if the list is in "view all articles" mode, while fade+slide animations
        //    run if the list is in "view only unread" mode, since once the item is marked
        //    read it needs to be removed from the list.
        // 2. Prevents a UI bug where MaterialContainerTransform has a hard time figuring
        //    out what to draw, since the shared element either has changed its opacity,
        //    or doesn't exist at all anymore.
        if (hasReadArticle) {
            NewsListAdapter.itemReadStatusChangedListener?.invoke(newsItemId, true)
        }

        binding.webView.apply {
            loadUrl("")
            stopLoading()
            destroy()
        }
    }

    private fun handleIntent(intent: Intent?) = when {
        intent == null -> false
        // This used to be a simple intent.extras != null check.
        // However, if the URL is clicked outside of the app, Android adds a "browser_id" extra,
        // which is set to the app's package name. So we need to explicitly check if the extras
        // we want (NEWS_ITEM_ID) or (DELAY_AD_START) are present.
        intent.hasExtra(INTENT_NEWS_ITEM_ID) || intent.hasExtra(INTENT_DELAY_AD_START) -> {
            shouldDelayAdStart = intent.getBooleanExtra(
                INTENT_DELAY_AD_START,
                false
            )
            newsItemId = intent.getLongExtra(
                INTENT_NEWS_ITEM_ID,
                -1L
            )
            true
        }
        Intent.ACTION_VIEW == intent.action -> intent.data.let {
            shouldDelayAdStart = true
            fullUrl = it.toString()

            if (it == null) {
                false
            } else {
                when (it.scheme) {
                    "http", "https" -> {
                        val path = it.path ?: return@let false
                        val groupValues = LINK_PATH_REGEX.matchEntire(path)?.groupValues ?: return@let false
                        if (groupValues.size > 1) {
                            newsItemId = try {
                                groupValues[1].toLong()
                            } catch (e: NumberFormatException) {
                                -1L
                            }
                            true
                        } else {
                            false
                        }
                    }
                    "oxygenupdater" -> {
                        // oxygenupdater://news/<id>

                        newsItemId = try {
                            it.lastPathSegment?.toLong() ?: -1L
                        } catch (e: NumberFormatException) {
                            -1L
                        }

                        newsItemId >= 0L
                        true
                    }
                    else -> false
                }
            }
        }
        else -> false
    }

    private fun setupAds() {
        if (newsViewModel.mayShowAds) {
            setupBannerAd()
            setupInterstitialAd()
        } else {
            // reset NestedScrollView padding
            binding.nestedScrollView.setPadding(0, 0, 0, 0)

            binding.newsArticleAdViewContainer.isVisible = false
        }
    }

    private fun setupBannerAd() {
        binding.newsArticleAdViewContainer.apply {
            isVisible = true
            bannerAdView.loadAd(buildAdRequest())
            bannerAdView.adListener = object : AdListener() {
                override fun onAdLoaded() = super.onAdLoaded().also {
                    logDebug(TAG, "Banner ad loaded")

                    // Need to add spacing between NestedScrollView contents and the AdView to avoid overlapping the last item
                    // Since the AdView's size is SMART_BANNER, bottom padding should be exactly the AdView's height,
                    // which can only be calculated once the AdView has been drawn on the screen
                    post { binding.nestedScrollView.updatePadding(bottom = height) }
                }
            }
        }
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
        buildAdRequest(),
        interstitialAdLoadCallback
    )

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadNewsItem() {
        showLoadingState()

        if (newsItemId <= 0L) {
            showErrorState(WebViewError(ERROR_BAD_URL), true)
        } else {
            // Obtain the contents of the news item
            // (to save data when loading the entire list of news items, only title & subtitle are returned there)
            newsViewModel.fetchNewsItem(newsItemId).observe(
                this,
                fetchNewsItemObserver
            )
        }
    }

    private fun showLoadingState() {
        binding.progressBar.isVisible = true

        val imageUrl = intent.getStringExtra(INTENT_NEWS_ITEM_IMAGE_URL)
        val title = intent.getStringExtra(INTENT_NEWS_ITEM_TITLE)
        val subtitle = intent.getStringExtra(INTENT_NEWS_ITEM_SUBTITLE)

        binding.collapsingToolbarImage.load(imageUrl) {
            placeholder(R.drawable.image)
            error(R.drawable.logo_notification)
        }

        binding.collapsingToolbarLayout.title = title ?: getString(R.string.loading)
        binding.collapsingToolbarLayout.subtitle = getString(R.string.summary_please_wait)
        binding.newsItemSubtitle.apply {
            subtitle.let {
                isVisible = it != null
                text = it
            }
        }
        binding.newsDatePublished.text = getString(R.string.loading)
    }

    private fun hideLoadingState() {
        // hide progress bar since the page has been loaded
        binding.progressBar.isVisible = false
    }

    private fun showErrorState(
        error: WebViewError? = null,
        isInvalidId: Boolean = false,
    ) {
        // hide progress bar since the page failed to load
        binding.progressBar.isVisible = false

        Dialogs.showArticleError(
            this,
            !isInvalidId,
            error?.errorCodeString ?: getString(R.string.error),
            if (isInvalidId) {
                HtmlCompat.fromHtml(
                    getString(R.string.news_load_id_error, fullUrl),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            } else {
                getString(R.string.news_load_network_error)
            }
        ) { loadNewsItem() }
    }

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

        const val INTENT_NEWS_ITEM_ID = "NEWS_ITEM_ID"
        const val INTENT_NEWS_ITEM_IMAGE_URL = "NEWS_ITEM_IMAGE_URL"
        const val INTENT_NEWS_ITEM_TITLE = "NEWS_ITEM_TITLE"
        const val INTENT_NEWS_ITEM_SUBTITLE = "NEWS_ITEM_SUBTITLE"
        const val INTENT_DELAY_AD_START = "DELAY_AD_START"
    }
}
