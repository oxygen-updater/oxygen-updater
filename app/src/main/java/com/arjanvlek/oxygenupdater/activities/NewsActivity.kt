package com.arjanvlek.oxygenupdater.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.OxygenUpdater.Companion.buildAdRequest
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.adapters.NewsAdapter
import com.arjanvlek.oxygenupdater.database.NewsDatabaseHelper
import com.arjanvlek.oxygenupdater.exceptions.NetworkException
import com.arjanvlek.oxygenupdater.internal.WebViewClient
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.AppLocale
import com.arjanvlek.oxygenupdater.models.AppLocale.NL
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.utils.Logger.logDebug
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.ThemeUtils
import com.arjanvlek.oxygenupdater.utils.Utils
import com.arjanvlek.oxygenupdater.viewmodels.NewsViewModel
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.InterstitialAd
import kotlinx.android.synthetic.main.activity_news.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.LocalDateTime

class NewsActivity : SupportActionBarActivity() {

    private val newsDatabaseHelper by inject<NewsDatabaseHelper>()
    private val newsViewModel by viewModel<NewsViewModel>()
    private val settingsManager by inject<SettingsManager>()

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
    private val fetchNewsItemObserver = Observer<NewsItem> { newsItem ->
        if (isFinishing) {
            return@Observer
        }

        if (newsItem == null || !newsItem.isFullyLoaded) {
            showErrorState()

            return@Observer
        }

        // hide the error layout
        errorLayout.isVisible = false

        val locale = AppLocale.get()

        // Display the title of the article.
        collapsingToolbarLayout.title = newsItem.getTitle(locale)
        // Display the name of the author of the article
        collapsingToolbarLayout.subtitle = newsItem.authorName

        Glide.with(this)
            .load(newsItem.imageUrl)
            .into(collapsingToolbarImage)

        // Display the contents of the article.
        webView.apply {
            // must be done to avoid the white background in dark themes
            setBackgroundColor(Color.TRANSPARENT)

            isVisible = true
            settings.javaScriptEnabled = true

            if (newsItem.getText(locale).isNullOrEmpty()) {
                loadDataWithBaseURL("", getString(R.string.news_empty), "text/html", "UTF-8", "")
            } else {
                val newsLanguage = if (locale == NL) "NL" else "EN"
                var newsContentUrl = BuildConfig.SERVER_BASE_URL + "news-content/" + newsItem.id + "/" + newsLanguage + "/"

                // since we can't edit CSS in WebViews,
                // append 'Light' or 'Dark' to newContentUrl to get the corresponding themed version
                // backend handles CSS according to material spec
                newsContentUrl += if (ThemeUtils.isNightModeActive(context)) "Dark" else "Light"

                settings.userAgentString = OxygenUpdater.APP_USER_AGENT
                loadUrl(newsContentUrl)
            }

            // disable loading state once page is completely loaded
            webViewClient = WebViewClient(context) {
                // hide progress bar since the page has been loaded
                hideLoadingState()
            }
        }

        // Display the last update time of the article.
        newsDatePublished.apply {
            if (newsItem.dateLastEdited == null && newsItem.datePublished != null) {
                isVisible = true
                text = getString(
                    R.string.news_date_published,
                    Utils.formatDateTime(this@NewsActivity, newsItem.datePublished)
                )
            } else if (newsItem.dateLastEdited != null) {
                isVisible = true
                text = getString(
                    R.string.news_date_published,
                    Utils.formatDateTime(this@NewsActivity, newsItem.dateLastEdited)
                )
            } else {
                isVisible = false
            }
        }

        // Mark the item as read on the device.
        newsDatabaseHelper.markNewsItemRead(newsItem)

        NewsAdapter.newsItemReadListener?.invoke(newsItem.id!!)

        // Mark the item as read on the server (to increase times read counter)
        newsViewModel.markNewsItemRead(newsItem.id!!).observe(this, markNewsItemReadObserver)
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        if (intent?.extras == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_news)

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
        webView.apply {
            loadUrl("")
            stopLoading()
            destroy()
        }
    }

    private fun setupAds() {
        if (newsViewModel.mayShowAds) {
            setupBannerAd()

            if (intent.getBooleanExtra(INTENT_DELAY_AD_START, false)) {
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
                        errorCode: Int
                    ) = super.onAdFailedToLoad(errorCode).also {
                        logDebug(TAG, "Interstitial ad failed to load: $errorCode")

                        // Store the last date when the ad was shown. Used to limit the ads to one per 5 minutes.
                        settingsManager.savePreference(
                            SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN,
                            LocalDateTime.now().toString()
                        )
                    }

                    override fun onAdClosed() = super.onAdClosed().also {
                        logDebug(TAG, "Interstitial ad closed")

                        // Store the last date when the ad was shown. Used to limit the ads to one per 5 minutes.
                        settingsManager.savePreference(
                            SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN,
                            LocalDateTime.now().toString()
                        )
                    }

                    override fun onAdLoaded() = super.onAdLoaded().also {
                        logDebug(TAG, "Interstitial ad loaded")

                        if (!isFinishing) {
                            show()
                        }
                    }
                }
            }
        }
    }

    private fun showLoadingState() {
        // hide the error layout
        errorLayout.isVisible = false

        progressBar.isVisible = true
        newsLayout.isVisible = false

        // Display the title of the article.
        collapsingToolbarLayout.title = getString(R.string.loading)
    }

    private fun hideLoadingState() {
        progressBar.isVisible = false
        newsLayout.isVisible = true
    }

    private fun showErrorState() {
        // hide progress bar since the page failed to load
        progressBar.isVisible = false

        // Hide the last updated view.
        newsDatePublished.isVisible = false
        // show the error layout
        errorLayout.isVisible = true

        newsRetryButton.setOnClickListener {
            loadNewsItem()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadNewsItem() {
        showLoadingState()

        // Obtain the contents of the news item
        // (to save data when loading the entire list of news items, only title & subtitle are returned there)
        newsViewModel.fetchNewsItem(
            intent.getLongExtra(INTENT_NEWS_ITEM_ID, -1L)
        ).observe(this, fetchNewsItemObserver)
    }

    override fun onBackPressed() = finish()

    /**
     * Respond to the action bar's Up/Home button.
     * Delegate to [onBackPressed] if [android.R.id.home] is clicked, otherwise call `super`
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> onBackPressed().let { true }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "NewsActivity"

        const val INTENT_NEWS_ITEM_ID = "NEWS_ITEM_ID"
        const val INTENT_DELAY_AD_START = "DELAY_AD_START"
    }
}
