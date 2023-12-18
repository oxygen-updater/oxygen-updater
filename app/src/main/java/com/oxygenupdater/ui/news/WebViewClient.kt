package com.oxygenupdater.ui.news

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.utils.ApiBaseUrl
import com.oxygenupdater.utils.logDebug

/**
 * Wrapper around [android.webkit.WebViewClient] with two responsibilities:
 * - Sync [WebViewState] to [WebView] callbacks
 * - Override default URL handling to launch an activity via [Intent.ACTION_VIEW].
 *   Default behaviour (when not overridden) is to open the link in the WebView
 *   itself, which is not desired because we want to keep our article as the
 *   only webpage being displayed.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class WebViewClient(private val context: Context) : android.webkit.WebViewClient() {

    lateinit var state: WebViewState

    /**
     * Updates [WebViewState.loadingState] to [LoadingState.Loading]
     * and resets [WebViewState.errorForCurrentRequest]
     */
    override fun onPageStarted(
        view: WebView,
        url: String?,
        favicon: Bitmap?,
    ) = super.onPageStarted(view, url, favicon).also {
        state.loadingState = LoadingState.Loading(0f)
        state.errorForCurrentRequest = null
    }

    /**
     * Sets [WebViewState.loadingState] to [LoadingState.Finished], but only
     * below API 23. See [onPageCommitVisible] for API >= 23.
     */
    override fun onPageFinished(view: WebView, url: String?) = super.onPageFinished(view, url).also {
        if (SDK_INT >= VERSION_CODES.M) return

        logDebug(TAG, "Page finished: $url")
        state.loadingState = LoadingState.Finished
    }

    /**
     * Sets [WebViewState.loadingState] to [LoadingState.Finished], but only
     * on API >= 23. See [onPageFinished] for API < 23.
     *
     * @see <a href="https://github.com/oxygen-updater/oxygen-updater/pull/108">PR#108</a>
     */
    override fun onPageCommitVisible(
        view: WebView,
        url: String?,
    ) = super.onPageCommitVisible(view, url).also {
        logDebug(TAG, "Page commit visible: $url")
        state.loadingState = LoadingState.Finished
    }

    /** Updates [WebViewState.canGoBack] */
    override fun doUpdateVisitedHistory(
        view: WebView,
        url: String?,
        isReload: Boolean,
    ) = super.doUpdateVisitedHistory(view, url, isReload).also {
        state.canGoBack = view.canGoBack()
    }

    /**
     * [Intent.ACTION_VIEW] should be enough to handle cases of opening page links
     * in the browser, as well as handle custom URL schemes (apps register
     * themselves to handle URIs: e.g. `mailto://`, `reddit://`, `tel://`, etc).
     *
     * This API was added in API 23, which is why [shouldOverrideUrlLoading] is
     * also implemented, so that behaviour remains the same across all API levels.
     *
     * @see <a href="https://github.com/oxygen-updater/oxygen-updater/issues/129">Issue#129</a>
     */
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest?): Boolean {
        if (request == null) return false
        if (SDK_INT >= VERSION_CODES.N && request.isRedirect) {
            return super.shouldOverrideUrlLoading(view, request)
        }

        val uri = request.url
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            // Fallback: copy to clipboard instead
            context.copyToClipboard(uri.toString())
        }

        return true
    }

    /** Used below API 23 */
    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
        if (url == null) return false

        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            // Fallback: copy to clipboard instead
            context.copyToClipboard(url)
        }

        return true
    }

    @RequiresApi(VERSION_CODES.M)
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest?,
        resourceError: WebResourceError?,
    ) = super.onReceivedError(view, request, resourceError).also {
        val requestUrl = request?.url?.toString()
        val error = WebViewError.from(resourceError)
        logDebug(TAG, "Received error for $requestUrl: $error")

        // Errors that aren't for our URLs are ignored, because WebView (especially on newer APIs) reports errors for
        // any resource (e.g. iframe, image, etc). Note that this is a carry-over from the old pre-Compose code, where
        // it was necessary to fix a bug that only a few users experienced — WebView loads the article successfully,
        // but also reports a "net::ERR_CONNECTION_REFUSED" error. Prelim investigation didn't lead anywhere, until we
        // discovered those errors originated from script URLs that are loaded within the main URL's page (e.g. Google
        // AdSense and Cloudflare Web Analytics). I'm not sure if this workaround is still needed; keeping it anyway.
        updateError(error, requestUrl)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String?,
        failingUrl: String?,
    ) = super.onReceivedError(view, errorCode, description, failingUrl).also {
        val error = WebViewError(errorCode, description)
        logDebug(TAG, "Received error for $failingUrl: $error")
        updateError(error, failingUrl)
    }

    @RequiresApi(VERSION_CODES.M)
    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) = super.onReceivedHttpError(view, request, errorResponse).also {
        val error = WebViewError.from(errorResponse)
        logDebug(TAG, "Received HTTP error: $error")
        updateError(error, request?.url?.toString())
    }

    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler?,
        sslError: SslError?,
    ) = super.onReceivedSslError(view, handler, sslError).also {
        val error = WebViewError.from(sslError)
        logDebug(TAG, "Received SSL error: $error")
        updateError(error, sslError?.url)
    }

    /**
     * Update [WebViewState.errorForCurrentRequest] only if it hasn't been set already.
     *
     * Errors that aren't for our URLs are ignored, because WebView (especially
     * on newer APIs) reports errors for any resource (e.g. iframe, image, etc).
     *
     * Since we use [WebView] only for news articles, we're checking if [url]
     * matches [ApiBaseUrl], in which case we're ignoring the error.
     * A more general match would be against hostname only.
     *
     * Note that carry-over from the old pre-Compose code, where it was necessary
     * to fix a bug that only a few users experience — WebView successfully loads
     * the article itself, but also reports a "net::ERR_CONNECTION_REFUSED" error.
     * Prelim investigation didn't lead anywhere, until we discovered those errors
     * originated from script URLs that are loaded within the main URL's page
     * (e.g. Google AdSense and Cloudflare Web Analytics).
     *
     * Not sure if this workaround is still needed; keeping it anyway.
     */
    private fun updateError(
        error: WebViewError,
        url: String?,
    ) = if (url?.startsWith(ApiBaseUrl) == true && state.errorForCurrentRequest == null) {
        state.errorForCurrentRequest = error
    } else logDebug(TAG, "Ignoring error for url: $url")

    companion object {
        private const val TAG = "WebViewClient"
    }
}
