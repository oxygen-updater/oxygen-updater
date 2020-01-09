package com.arjanvlek.oxygenupdater.faq

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.arjanvlek.oxygenupdater.ApplicationData
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

        faq_webpage_layout.apply {
            setColorSchemeResources(R.color.colorPrimary)
            setOnRefreshListener { loadFaqPage() }
        }

        loadFaqPage()
    }

    /**
     * Handles action bar item clicks
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button, exit the activity gracefully to prevent downloads getting stuck
        if (item.itemId == android.R.id.home) {
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
            faq_webpage_layout.isRefreshing = true

            switchViews(true)

            faqWebView.apply {
                // since we can't edit CSS in WebViews,
                // append 'Light' or 'Dark' to faqServerUrl to get the corresponding themed version
                // backend handles CSS according to material spec
                val faqServerUrl = BuildConfig.FAQ_SERVER_URL + "/" + if (ThemeUtils.isNightModeActive(context)) "Dark" else "Light"

                settings.javaScriptEnabled = true
                settings.userAgentString = ApplicationData.APP_USER_AGENT
                clearCache(true)
                loadUrl(faqServerUrl)
            }

            faq_webpage_layout.isRefreshing = false
        } else {
            switchViews(false)
        }
    }

    /**
     * Handler for the Retry button on the "No network connection" page.
     *
     * @param v View
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    fun onRetryButtonClick(v: View?) {
        loadFaqPage()
    }

    /**
     * Switches between the Web Browser view and the No Network connection screen based on the
     * hasNetwork parameter.
     *
     * @param hasNetwork Whether the device has a network connection or not.
     */
    private fun switchViews(hasNetwork: Boolean) {
        faq_no_network_view.visibility = if (hasNetwork) GONE else VISIBLE
        faq_webpage_layout.visibility = if (hasNetwork) VISIBLE else GONE
    }
}
