package com.arjanvlek.oxygenupdater.news

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.buildAdRequest
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.ThemeUtils
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.WebViewClient
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.server.NetworkException
import com.arjanvlek.oxygenupdater.models.AppLocale
import com.arjanvlek.oxygenupdater.models.AppLocale.NL
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.views.NewsAdapter
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.InterstitialAd
import kotlinx.android.synthetic.main.activity_news.*

class NewsActivity : SupportActionBarActivity() {

    @SuppressLint("SetJavaScriptEnabled") // JS is required to load videos and other dynamic content.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null || intent.extras == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_news)

        setupAds()

        enableLoadingState()

        loadNewsItem()
    }

    private fun setupAds() {
        Utils.checkAdSupportStatus(this) { adsAreSupported ->
            if (adsAreSupported) {
                newsArticleAdView.apply {
                    isVisible = true
                    loadAd(buildAdRequest())
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()

                            // need to add spacing between NestedScrollView contents and the AdView to avoid overlapping the last item
                            // Since the AdView's size is SMART_BANNER, bottom padding should be exactly the AdView's height,
                            // which can only be calculated once the AdView has been drawn on the screen
                            post { nestedScrollView.updatePadding(bottom = height) }
                        }
                    }
                }

                // Delayed display of ad if coming from a notification. Otherwise ad is displayed when transitioning from NewsFragment.
                if (intent.getBooleanExtra(INTENT_START_WITH_AD, false)) {
                    InterstitialAd(application).apply {
                        adUnitId = getString(R.string.advertising_interstitial_unit_id)
                        loadAd(buildAdRequest())

                        // The ad will be shown after 5 seconds.
                        Handler().postDelayed({
                            if (!isFinishing) {
                                show()
                            }
                        }, 5000)
                    }
                }
            } else {
                // reset NestedScrollView padding
                nestedScrollView.setPadding(0, 0, 0, 0)

                newsArticleAdView.isVisible = false
            }
        }
    }

    fun enableLoadingState() {
        progressBar.isVisible = true
        newsLayout.isVisible = false

        // Display the title of the article.
        collapsingToolbarLayout.title = getString(R.string.loading)
    }

    fun disableLoadingState() {
        progressBar.isVisible = false
        newsLayout.isVisible = true
    }

    override fun onResume() {
        webView.onResume()
        super.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.apply {
            loadUrl("")
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }

    private fun loadNewsItem() {
        loadNewsItem(0)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadNewsItem(retryCount: Int) {
        val intent = intent
        val applicationData = application as ApplicationData

        // Obtain the contents of the news item (to save data when loading the entire list of news items, only title + subtitle are returned there).
        applicationData.serverConnector!!.getNewsItem(applicationData, intent.getLongExtra(INTENT_NEWS_ITEM_ID, -1L)) { newsItem ->
            if (isFinishing || isDestroyed) {
                return@getNewsItem
            }

            if (newsItem == null || !newsItem.isFullyLoaded) {
                if (Utils.checkNetworkConnection(applicationData) && retryCount < 5) {
                    loadNewsItem(retryCount + 1)
                } else {
                    webView.apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        loadDataWithBaseURL("", getString(R.string.news_load_error), "text/html", "UTF-8", "")
                    }

                    // Hide the last updated view.
                    newsDatePublished.isVisible = false

                    newsRetryButton.apply {
                        isVisible = true
                        setOnClickListener { loadNewsItem(1) }
                    }
                }

                return@getNewsItem
            }

            newsRetryButton.isVisible = false

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

                    settings.userAgentString = ApplicationData.APP_USER_AGENT
                    loadUrl(newsContentUrl)
                }

                // disable loading state once page is completely loaded
                webViewClient = WebViewClient(context) {
                    // hide progress bar since the page has been loaded
                    disableLoadingState()
                }
            }

            // Display the last update time of the article.
            newsDatePublished.apply {
                if (newsItem.dateLastEdited == null && newsItem.datePublished != null) {
                    isVisible = true
                    text = getString(R.string.news_date_published, Utils.formatDateTime(application, newsItem.datePublished))
                } else if (newsItem.dateLastEdited != null) {
                    isVisible = true
                    text = getString(R.string.news_date_published, Utils.formatDateTime(application, newsItem.dateLastEdited))
                } else {
                    isVisible = false
                }
            }

            // Mark the item as read on the device.
            NewsDatabaseHelper(application).apply {
                markNewsItemAsRead(newsItem)
                close()
            }

            NewsAdapter.newsItemReadListener.invoke(newsItem.id!!)

            // Mark the item as read on the server (to increase times read counter)
            if (application != null && application is ApplicationData && Utils.checkNetworkConnection(application)) {
                (application as ApplicationData).serverConnector!!.markNewsItemAsRead(newsItem.id) { result: ServerPostResult? ->
                    if (result?.success == false) {
                        logError("NewsActivity", NetworkException("Error marking news item as read on the server:" + result.errorMessage))
                    }
                }
            }
        }
    }

    override fun onBackPressed() = finish()

    /**
     * Respond to the action bar's Up/Home button
     */
    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
        finish()
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    companion object {
        const val INTENT_NEWS_ITEM_ID = "NEWS_ITEM_ID"
        const val INTENT_START_WITH_AD = "START_WITH_AD"
    }
}
