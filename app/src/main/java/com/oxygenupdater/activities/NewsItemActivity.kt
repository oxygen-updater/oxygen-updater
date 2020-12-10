package com.oxygenupdater.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_SHOW_DATE
import android.text.format.DateUtils.FORMAT_SHOW_TIME
import android.text.format.DateUtils.FORMAT_SHOW_YEAR
import android.text.method.LinkMovementMethod
import android.webkit.WebViewClient.ERROR_BAD_URL
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.LoadAdError
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.OxygenUpdater.Companion.buildAdRequest
import com.oxygenupdater.R
import com.oxygenupdater.adapters.NewsListAdapter
import com.oxygenupdater.exceptions.NetworkException
import com.oxygenupdater.internal.WebViewClient
import com.oxygenupdater.internal.WebViewError
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.ServerPostResult
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.ThemeUtils
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.viewmodels.NewsViewModel
import kotlinx.android.synthetic.main.activity_news_item.*
import kotlinx.android.synthetic.main.layout_error.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.LocalDateTime

class NewsItemActivity : SupportActionBarActivity(
    R.layout.activity_news_item,
    MainActivity.PAGE_NEWS
) {

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

        hideErrorStateIfInflated()

        // Display the title of the article.
        collapsingToolbarLayout.title = newsItem.title
        // Display the name of the author of the article
        collapsingToolbarLayout.subtitle = newsItem.authorName

        Glide.with(this)
            .load(newsItem.imageUrl)
            .placeholder(R.drawable.image)
            .error(R.drawable.logo_notification)
            .into(collapsingToolbarImage)

        // Display the contents of the article.
        webView.apply {
            // must be done to avoid the white background in dark themes
            setBackgroundColor(Color.TRANSPARENT)

            isVisible = true
            settings.javaScriptEnabled = true

            if (newsItem.text.isNullOrEmpty()) {
                loadDataWithBaseURL("", getString(R.string.news_empty), "text/html", "UTF-8", "")
            } else {
                // since we can't edit CSS in WebViews,
                // append 'Light' or 'Dark' to newsContentUrl to get the corresponding themed version
                // backend handles CSS according to material spec
                fullUrl = newsItem.url + if (ThemeUtils.isNightModeActive(context)) "Dark" else "Light"

                settings.userAgentString = OxygenUpdater.APP_USER_AGENT
                loadUrl(fullUrl)
            }

            // disable loading state once page is completely loaded
            webViewClient = WebViewClient(context) { error ->
                hideLoadingState()

                if (error == null) {
                    hideErrorStateIfInflated()
                } else {
                    showErrorState(error)
                }
            }
        }

        // Display the subtitle of the article.
        newsItemSubtitle.apply {
            newsItem.subtitle.let {
                isVisible = it != null
                text = it
            }
        }

        // Display the last update time of the article.
        newsDatePublished.apply {
            val dateTimePrefix = if (newsItem.dateLastEdited == null && newsItem.datePublished != null) {
                newsItem.datePublished
            } else {
                newsItem.dateLastEdited
            }?.let {
                val userDateTime = LocalDateTime.parse(it.replace(" ", "T"))
                    .atZone(Utils.SERVER_TIME_ZONE)

                text = DateUtils.getRelativeTimeSpanString(
                    userDateTime.toInstant().toEpochMilli(),
                    System.currentTimeMillis(),
                    DateUtils.SECOND_IN_MILLIS,
                    FORMAT_SHOW_TIME or FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR
                )
            }

            isVisible = dateTimePrefix != null
        }

        hasReadArticle = true
        // Mark the item as read on the server (to increase times read counter)
        newsViewModel.markNewsItemRead(newsItem).observe(this, markNewsItemReadObserver)
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        if (!handleIntent(intent)) {
            onBackPressed()
            return
        }

        setupAds()
        loadNewsItem()
    }

    override fun onResume() = super.onResume().also {
        webView.onResume()
    }

    override fun onPause() = super.onPause().also {
        webView.onPause()
    }

    override fun onDestroy() = super.onDestroy().also {
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

        webView.apply {
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
                        // https://oxygenupdater.com/api/<version>/news-content/<id>/<lang>/<theme>
                        val index = it.pathSegments.indexOf("news-content") + 1

                        if (index != 0 && index < it.pathSegments.size) {
                            newsItemId = try {
                                it.pathSegments[index].toLong()
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

            if (shouldDelayAdStart) {
                logDebug(TAG, "Setting up interstitial ad in 5s")

                Handler().postDelayed(
                    {
                        if (!isFinishing) {
                            setupInterstitialAd()
                        }
                    }, 5000
                )
            } else {
                setupInterstitialAd()
            }
        } else {
            // reset NestedScrollView padding
            nestedScrollView.setPadding(0, 0, 0, 0)

            newsArticleAdView.isVisible = false
        }
    }

    private fun setupBannerAd() {
        newsArticleAdView.apply {
            isVisible = true
            loadAd(buildAdRequest())
            adListener = object : AdListener() {
                override fun onAdLoaded() = super.onAdLoaded().also {
                    logDebug(TAG, "Banner ad loaded")

                    // Need to add spacing between NestedScrollView contents and the AdView to avoid overlapping the last item
                    // Since the AdView's size is SMART_BANNER, bottom padding should be exactly the AdView's height,
                    // which can only be calculated once the AdView has been drawn on the screen
                    post { nestedScrollView.updatePadding(bottom = height) }
                }
            }
        }
    }

    private fun setupInterstitialAd() {
        if (newsViewModel.mayShowInterstitialAd) {
            InterstitialAd(this).apply {
                adUnitId = getString(R.string.advertising_interstitial_unit_id)
                loadAd(buildAdRequest())

                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(
                        loadAdError: LoadAdError?
                    ) = super.onAdFailedToLoad(loadAdError).also {
                        logDebug(TAG, "Interstitial ad failed to load: $loadAdError")
                    }

                    override fun onAdClosed() = super.onAdClosed().also {
                        logDebug(TAG, "Interstitial ad closed")
                    }

                    override fun onAdLoaded() = super.onAdLoaded().also {
                        logDebug(TAG, "Interstitial ad loaded")

                        if (!isFinishing) {
                            // Store the last date when the ad was shown. Used to limit the ads to one per 5 minutes.
                            SettingsManager.savePreference(
                                SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN,
                                LocalDateTime.now().toString()
                            )

                            show()
                        }
                    }
                }
            }
        }
    }

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
        progressBar.isVisible = true
        newsLayout.isVisible = true

        hideErrorStateIfInflated()

        val imageUrl = intent.getStringExtra(INTENT_NEWS_ITEM_IMAGE_URL)
        val title = intent.getStringExtra(INTENT_NEWS_ITEM_TITLE)
        val subtitle = intent.getStringExtra(INTENT_NEWS_ITEM_SUBTITLE)

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.image)
            .error(R.drawable.logo_notification)
            .into(collapsingToolbarImage)

        collapsingToolbarLayout.title = title ?: getString(R.string.loading)
        collapsingToolbarLayout.subtitle = getString(R.string.summary_please_wait)
        newsItemSubtitle.apply {
            subtitle.let {
                isVisible = it != null
                text = it
            }
        }
        newsDatePublished.text = getString(R.string.loading)
    }

    private fun hideLoadingState() {
        // hide progress bar since the page has been loaded
        progressBar.isVisible = false
        // Show newsLayout, which contains the WebView
        newsLayout.isVisible = true
    }

    private fun showErrorState(
        error: WebViewError? = null,
        isInvalidId: Boolean = false
    ) {
        // hide progress bar since the page failed to load
        progressBar.isVisible = false
        newsLayout.isVisible = false

        inflateAndShowErrorState(error, isInvalidId)
    }

    private fun inflateAndShowErrorState(
        error: WebViewError?,
        isInvalidId: Boolean
    ) {
        collapsingToolbarLayout.title = getString(R.string.error)

        // Show error layout
        errorLayoutStub?.inflate()
        errorLayout.isVisible = true

        if (error != null) {
            errorTitle.apply {
                isVisible = true
                text = error.errorCodeString
            }
        } else {
            errorTitle.isVisible = false
        }

        if (isInvalidId) {
            errorText.apply {
                text = HtmlCompat.fromHtml(
                    getString(R.string.news_load_id_error, fullUrl),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
                // Make the links clickable
                movementMethod = LinkMovementMethod.getInstance()
            }
            errorActionButton.apply {
                isVisible = false
                setOnClickListener(null)
            }
        } else {
            errorText.apply {
                text = getString(R.string.news_load_network_error)
                movementMethod = null
            }
            errorActionButton.apply {
                isVisible = true
                setOnClickListener { loadNewsItem() }
            }
        }
    }

    private fun hideErrorStateIfInflated() {
        // Stub is null only after it has been inflated, and
        // we need to hide the error state only if it has been inflated
        if (errorLayoutStub == null) {
            errorLayout.isVisible = false
            errorActionButton.setOnClickListener { }
        }
    }

    companion object {
        private const val TAG = "NewsActivity"

        const val INTENT_NEWS_ITEM_ID = "NEWS_ITEM_ID"
        const val INTENT_NEWS_ITEM_IMAGE_URL = "NEWS_ITEM_IMAGE_URL"
        const val INTENT_NEWS_ITEM_TITLE = "NEWS_ITEM_TITLE"
        const val INTENT_NEWS_ITEM_SUBTITLE = "NEWS_ITEM_SUBTITLE"
        const val INTENT_DELAY_AD_START = "DELAY_AD_START"
    }
}
