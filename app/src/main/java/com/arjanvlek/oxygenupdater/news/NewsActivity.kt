package com.arjanvlek.oxygenupdater.news

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.ThemeUtils
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.i18n.Locale
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.server.NetworkException
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity
import com.google.android.gms.ads.InterstitialAd
import java8.util.function.Consumer

class NewsActivity : SupportActionBarActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override // JS is required to load videos and other dynamic content.
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null || intent.extras == null) {
            finish()
            return
        }

        setContentView(R.layout.loading)

        loadNewsItem()
    }

    private fun loadNewsItem() {
        loadNewsItem(0)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadNewsItem(retryCount: Int) {
        val applicationData = application as ApplicationData
        // Obtain the contents of the news item (to save data when loading the entire list of news items, only title + subtitle are returned there).
        applicationData.getServerConnector()
                .getNewsItem(applicationData, intent.getLongExtra(INTENT_NEWS_ITEM_ID, -1L), Consumer { newsItem ->

                    if (retryCount == 0) {
                        setContentView(R.layout.activity_news)
                    }

                    if (newsItem == null || !newsItem.isFullyLoaded) {
                        if (Utils.checkNetworkConnection(applicationData) && retryCount < 5) {
                            loadNewsItem(retryCount + 1)
                        } else {
                            val newsContents = getString(R.string.news_load_error)

                            val contentView = findViewById<WebView>(R.id.newsContent)
                            contentView.loadDataWithBaseURL("", newsContents, "text/html", "UTF-8", "")

                            // Hide the title, author name and last updated views.
                            findViewById<View>(R.id.newsTitle).visibility = View.GONE
                            findViewById<View>(R.id.newsDatePublished).visibility = View.GONE
                            findViewById<View>(R.id.newsAuthor).visibility = View.GONE

                            val retryButton = findViewById<Button>(R.id.newsRetryButton)
                            retryButton.visibility = View.VISIBLE
                            retryButton.setOnClickListener { loadNewsItem(1) }
                        }
                        return@Consumer
                    }

                    findViewById<View>(R.id.newsRetryButton).visibility = View.GONE

                    val locale = Locale.locale

                    // Mark the item as read on the device.
                    val helper = NewsDatabaseHelper(application)
                    helper.markNewsItemAsRead(newsItem)
                    helper.close()

                    // Display the title of the article.
                    val titleView = findViewById<TextView>(R.id.newsTitle)
                    titleView.visibility = View.VISIBLE
                    titleView.text = newsItem.getTitle(locale)

                    // Display the contents of the article.
                    val contentView = findViewById<WebView>(R.id.newsContent)
                    contentView.visibility = View.VISIBLE
                    contentView.settings.javaScriptEnabled = true

                    var newsContents = newsItem.getText(locale)

                    if (newsContents!!.isEmpty()) {
                        newsContents = getString(R.string.news_empty)
                        contentView.loadDataWithBaseURL("", newsContents, "text/html", "UTF-8", "")
                    } else {
                        val newsLanguage = if (locale == Locale.NL) "NL" else "EN"
                        var newsContentUrl = BuildConfig.SERVER_BASE_URL + "news-content/" + newsItem.id + "/" + newsLanguage + "/"

                        // since we can't edit CSS in WebViews,
                        // append 'Light' or 'Dark' to newContentUrl to get the corresponding themed version
                        // backend handles CSS according to material spec
                        newsContentUrl += if (ThemeUtils.isNightModeActive(this))
                            "Dark"
                        else
                            "Light"

                        contentView.settings.userAgentString = ApplicationData.APP_USER_AGENT
                        contentView.loadUrl(newsContentUrl)
                    }

                    // Display the name of the author of the article
                    val authorView = findViewById<TextView>(R.id.newsAuthor)
                    if (newsItem.authorName != null && newsItem.authorName?.isNotEmpty()) {
                        authorView.visibility = View.VISIBLE
                        authorView.text = getString(R.string.news_author, newsItem.authorName)
                    } else {
                        authorView.visibility = View.GONE
                    }

                    // Display the last update time of the article.
                    val datePublishedView = findViewById<TextView>(R.id.newsDatePublished)

                    if (newsItem.dateLastEdited == null && newsItem.datePublished != null) {
                        datePublishedView.visibility = View.VISIBLE
                        datePublishedView.text = getString(R.string.news_date_published, Utils.formatDateTime(application, newsItem
                                .datePublished))
                    } else if (newsItem.dateLastEdited != null) {
                        datePublishedView.visibility = View.VISIBLE
                        datePublishedView.text = getString(R.string.news_date_published, Utils.formatDateTime(application, newsItem
                                .dateLastEdited))
                    } else {
                        datePublishedView.visibility = View.GONE
                        titleView.visibility = View.GONE
                    }

                    // Mark the item as read on the server (to increase times read counter)
                    if (application != null && application is ApplicationData && Utils
                                    .checkNetworkConnection(application)) {
                        (application as ApplicationData).mServerConnector?.markNewsItemAsRead(newsItem.id!!, Consumer { result ->
                                    if (result != null && !result.isSuccess) {
                                        logError("NewsActivity", NetworkException("Error marking news item as read on the server:" + result.errorMessage!!))
                                    }

                                    // Delayed display of ad if coming from a notification. Otherwise ad is displayed when transitioning from NewsFragment.
                                    if (intent.getBooleanExtra(INTENT_START_WITH_AD, false) && !SettingsManager(application).getPreference(SettingsManager.PROPERTY_AD_FREE, false)) {
                                        val interstitialAd = InterstitialAd(application)
                                        interstitialAd.adUnitId = getString(R.string.news_ad_unit_id)
                                        interstitialAd.loadAd(ApplicationData.buildAdRequest())

                                        // The ad will be shown after 10 seconds.
                                        Handler().postDelayed({ interstitialAd.show() }, 10000)
                                    }
                                })
                    }
                })
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        val INTENT_NEWS_ITEM_ID = "NEWS_ITEM_ID"
        val INTENT_START_WITH_AD = "START_WITH_AD"
    }
}
