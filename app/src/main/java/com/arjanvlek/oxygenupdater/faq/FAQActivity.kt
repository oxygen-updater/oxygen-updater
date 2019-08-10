package com.arjanvlek.oxygenupdater.faq

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.APP_USER_AGENT
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.ThemeUtils
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity
import kotlinx.android.synthetic.main.activity_faq.*

class FAQActivity : SupportActionBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)
    }

    override fun onStart() {
        super.onStart()

        faq_webpage_layout.setColorSchemeResources(R.color.colorPrimary)
        faq_webpage_layout.setOnRefreshListener { this.loadFaqPage() }

        loadFaqPage()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handles action bar item clicks
        val id = item.itemId

        // Respond to the action bar's Up/Home button, exit the activity gracefully to prevent downloads getting stuck
        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Gracefully quit the activity if the Back button is pressed to speed the app up
     */
    override fun onBackPressed() {
        finish()
    }

    /**
     * Loads the FAQ page, or displays a No Network connection screen if there is no network connection
     */
    @SuppressLint("SetJavaScriptEnabled") // JavaScript is required to toggle the FAQ Item boxes.
    private fun loadFaqPage() {
        if (Utils.checkNetworkConnection(applicationContext)) {
            val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.faq_webpage_layout)

            refreshLayout.isRefreshing = true

            val faqPageView = findViewById<WebView>(R.id.faqWebView)

            switchViews(true)

            faqPageView.settings.javaScriptEnabled = true
            faqPageView.settings.userAgentString = APP_USER_AGENT
            faqPageView.clearCache(true)

            var faqServerUrl = BuildConfig.FAQ_SERVER_URL + "/"

            // since we can't edit CSS in WebViews,
            // append 'Light' or 'Dark' to faqServerUrl to get the corresponding themed version
            // backend handles CSS according to material spec
            faqServerUrl += if (ThemeUtils.isNightModeActive(this))
                "Dark"
            else
                "Light"

            faqPageView.loadUrl(faqServerUrl)

            refreshLayout.isRefreshing = false
        } else {
            switchViews(false)
        }
    }

    /**
     * Handler for the Retry button on the "No network connection" page.
     *
     * @param v View
     */
    @Suppress("UNUSED_PARAMETER")
    fun onRetryButtonClick(v: View) {
        loadFaqPage()
    }

    /**
     * Switches between the Web Browser view and the No Network connection screen based on the
     * hasNetwork parameter.
     *
     * @param hasNetwork Whether the device has a network connection or not.
     */
    private fun switchViews(hasNetwork: Boolean) {
        val noNetworkLayout = findViewById<LinearLayout>(R.id.faq_no_network_view)
        noNetworkLayout.visibility = if (hasNetwork) View.GONE else View.VISIBLE

        val webPageLayout = findViewById<SwipeRefreshLayout>(R.id.faq_webpage_layout)
        webPageLayout.visibility = if (hasNetwork) View.VISIBLE else View.GONE
    }
}
