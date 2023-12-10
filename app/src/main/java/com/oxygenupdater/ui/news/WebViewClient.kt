package com.oxygenupdater.ui.news

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.google.accompanist.web.AccompanistWebViewClient
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.utils.ApiBaseUrl
import com.oxygenupdater.utils.logDebug

/**
 * Needed for handling page load finish events.
 * Note: Overriding [shouldOverrideUrlLoading] is necessary, as without it, custom URL schemes won't work (mailto, tel, etc)
 * Moreover, clicking page links in the associated WebView will load those pages within the WebView, which is not desired.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class WebViewClient(private val context: Context) : AccompanistWebViewClient() {

    /**
     * [Intent.ACTION_VIEW] should be enough to handle cases of opening page links in the browser,
     * as well as handle custom URL schemes (apps register themselves to handle URIs: e.g. `mailto://`, `reddit://`, `tel://`, etc)
     *
     * This API was added in API 23, which is why [shouldOverrideUrlLoading] is also implemented,
     * so that behaviour remains the same across all API levels.
     *
     * See [Issue#129](https://github.com/oxygen-updater/oxygen-updater/issues/129)
     */
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ) = request?.run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRedirect)
            return super.shouldOverrideUrlLoading(view, this)

        val uri = url
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            // Fallback: copy to clipboard instead
            context.copyToClipboard(uri.toString())
        }

        true
    } ?: super.shouldOverrideUrlLoading(view, request)

    /**
     * Used on API 22 and below
     */
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        url: String?,
    ) = url?.run {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, toUri()))
        } catch (e: ActivityNotFoundException) {
            // Fallback: copy to clipboard instead
            context.copyToClipboard(url)
        }

        true
    } ?: super.shouldOverrideUrlLoading(view, url)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        val requestUrl = request?.url?.toString()
        if (requestUrl == null || error == null) return

        logDebug(TAG, "Received error for $requestUrl: $error")
        // Errors that aren't for our URLs are ignored, because WebView (especially on newer APIs) reports errors for
        // any resource (e.g. iframe, image, etc). Note that this is a carry-over from the old pre-Compose code, where
        // it was necessary to fix a bug that only a few users experienced — WebView loads the article successfully,
        // but also reports a "net::ERR_CONNECTION_REFUSED" error. Prelim investigation didn't lead anywhere, until we
        // discovered those errors originated from script URLs that are loaded within the main URL's page (e.g. Google
        // AdSense and Cloudflare Web Analytics). I'm not sure if this workaround is still needed; keeping it anyway.
        if (requestUrl.startsWith(ApiBaseUrl)) super.onReceivedError(view, request, error)
    }

    companion object {
        private const val TAG = "WebViewClient"
    }
}
