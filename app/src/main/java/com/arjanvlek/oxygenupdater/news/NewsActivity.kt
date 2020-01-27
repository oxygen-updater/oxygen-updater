package com.arjanvlek.oxygenupdater.news

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.WebView
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.buildAdRequest
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.ThemeUtils
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.server.NetworkException
import com.arjanvlek.oxygenupdater.models.AppLocale
import com.arjanvlek.oxygenupdater.models.AppLocale.NL
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_AD_FREE
import com.arjanvlek.oxygenupdater.views.NewsAdapter
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity
import com.google.android.gms.ads.InterstitialAd
import kotlinx.android.synthetic.main.activity_news.*

class NewsActivity : SupportActionBarActivity() {
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled") // JS is required to load videos and other dynamic content.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null || intent.extras == null) {
            finish()
            return
        }

        setContentView(R.layout.loading)
        loadNewsItem()
    }

    override fun onResume() {
        webView?.onResume()
        super.onResume()
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView?.apply {
            loadUrl("")
            stopLoading()
            destroy()
        }

        webView = null
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
            if (retryCount == 0) {
                setContentView(R.layout.activity_news)
            }

            if (newsItem == null || !newsItem.isFullyLoaded) {
                if (Utils.checkNetworkConnection(applicationData) && retryCount < 5) {
                    loadNewsItem(retryCount + 1)
                } else {
                    newsContent?.apply {
                        webView = this
                        loadDataWithBaseURL("", getString(R.string.news_load_error), "text/html", "UTF-8", "")
                    }

                    // Hide the title, author name and last updated views.
                    newsTitle.visibility = GONE
                    newsDatePublished.visibility = GONE
                    newsAuthor.visibility = GONE

                    newsRetryButton.apply {
                        visibility = VISIBLE
                        setOnClickListener { loadNewsItem(1) }
                    }
                }

                return@getNewsItem
            }

            newsRetryButton.visibility = GONE

            val locale = AppLocale.get()

            // Display the title of the article.
            newsTitle.apply {
                visibility = VISIBLE
                text = newsItem.getTitle(locale)
            }

            // Display the contents of the article.
            newsContent.apply {
                webView = this

                visibility = VISIBLE
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
            }

            // Display the name of the author of the article
            newsAuthor.apply {
                if (!newsItem.authorName.isNullOrEmpty()) {
                    visibility = VISIBLE
                    text = getString(R.string.news_author, newsItem.authorName)
                } else {
                    visibility = GONE
                }
            }

            // Display the last update time of the article.
            newsDatePublished.apply {
                if (newsItem.dateLastEdited == null && newsItem.datePublished != null) {
                    visibility = VISIBLE
                    text = getString(R.string.news_date_published, Utils.formatDateTime(application, newsItem.datePublished))
                } else if (newsItem.dateLastEdited != null) {
                    visibility = VISIBLE
                    text = getString(R.string.news_date_published, Utils.formatDateTime(application, newsItem.dateLastEdited))
                } else {
                    visibility = GONE
                    newsTitle.visibility = GONE
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
                    if (result != null && !result.success) {
                        logError("NewsActivity", NetworkException("Error marking news item as read on the server:" + result.errorMessage))
                    }

                    // Delayed display of ad if coming from a notification. Otherwise ad is displayed when transitioning from NewsFragment.
                    if (intent.getBooleanExtra(INTENT_START_WITH_AD, false) && !SettingsManager(application).getPreference(PROPERTY_AD_FREE, false)) {
                        InterstitialAd(application).apply {
                            adUnitId = getString(R.string.advertising_interstitial_unit_id)
                            loadAd(buildAdRequest())

                            // The ad will be shown after 10 seconds.
                            Handler().postDelayed({ show() }, 10000)
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        finish()
    }

    /**
     * Respond to the action bar's Up/Home button
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val INTENT_NEWS_ITEM_ID = "NEWS_ITEM_ID"
        const val INTENT_START_WITH_AD = "START_WITH_AD"
    }
}
