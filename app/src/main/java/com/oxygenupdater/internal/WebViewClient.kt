package com.oxygenupdater.internal

import android.content.Context
import android.content.Intent
import android.net.http.SslError
import android.os.Build
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.utils.Logger.logDebug

/**
 * Needed for handling page load finish events.
 * Note: Overriding [shouldOverrideUrlLoading] is necessary, as without it, custom URL schemes won't work (mailto, tel, etc)
 * Moreover, clicking page links in the associated WebView will load those pages within the WebView, which is not desired.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class WebViewClient(
    private val context: Context,
    private val pageCommitVisibleCallback: KotlinCallback<WebViewError?>
) : WebViewClient() {

    private var error: WebViewError? = null

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
        request: WebResourceRequest?
    ) = request?.run {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url)
        ).let { true }
    } ?: super.shouldOverrideUrlLoading(view, request)

    /**
     * Used on API 22 and below
     */
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        url: String?
    ) = url?.run {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, toUri())
        ).let { true }
    } ?: super.shouldOverrideUrlLoading(view, url)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        resourceError: WebResourceError?
    ) = super.onReceivedError(view, request, resourceError).also {
        val error = WebViewError.from(resourceError)

        logDebug(TAG, "Received error: $error")
        updateError(error, request?.url?.toString())
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) = super.onReceivedError(view, errorCode, description, failingUrl).also {
        val error = WebViewError(errorCode, description)

        logDebug(TAG, "Received error: $error")
        updateError(error, failingUrl)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) = super.onReceivedHttpError(view, request, errorResponse).also {
        val error = WebViewError.from(errorResponse)

        logDebug(TAG, "Received HTTP error: $error")
        updateError(error, request?.url?.toString())
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        sslError: SslError?
    ) = super.onReceivedSslError(view, handler, sslError).also {
        val error = WebViewError.from(sslError)

        logDebug(TAG, "Received SSL error: $error")
        updateError(error, sslError?.url)
    }

    /**
     * Just invokes the callback, nothing special
     * This API was added in API 23, which is why [onPageFinished] is also implemented,
     * so that the callback is invoked on API 21 & 22 too.
     *
     * See [PR#108](https://github.com/oxygen-updater/oxygen-updater/pull/108)
     */
    override fun onPageCommitVisible(
        view: WebView?,
        url: String?
    ) = super.onPageCommitVisible(view, url).also {
        logDebug(TAG, "Page commit visible for: $url")
        pageCommitVisibleCallback.invoke(error)
    }

    /**
     * Just invokes the callback, nothing special
     * Used on API 22 and below
     */
    override fun onPageFinished(
        view: WebView?,
        url: String?
    ) = super.onPageFinished(view, url).also {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            logDebug(TAG, "Page finished for: $url")
            pageCommitVisibleCallback.invoke(error)
        }
    }

    /**
     * Set [error] only if it hasn't been set already.
     *
     * Errors that aren't for our URLs are ignored, because WebView (especially
     * on newer APIs) reports errors for any resource (e.g. iframe, image, etc).
     *
     * Since we use WebView only for news articles, we're checking if [url]
     * matches [API_BASE_URL], in which case we're ignoring the
     * error. A more general match would be against hostname only.
     *
     * Note that this is necessary to fix a bug that only a few users experience
     * — WebView successfully loads the article itself, but also reports a
     * "net::ERR_CONNECTION_REFUSED" error. Prelim investigation didn't lead
     * anywhere, until we discovered those errors originated from script URLs
     * that are loaded within the main URL's page (e.g. Google AdSense and
     * Cloudflare Web Analytics).
     */
    private fun updateError(error: WebViewError, url: String?) {
        if (url?.startsWith(API_BASE_URL) == false) {
            logDebug(TAG, "Ignoring error for url: $url")
        } else if (this.error == null) {
            this.error = error
        }
    }

    companion object {
        private const val TAG = "WebViewClient"
        private const val API_BASE_URL = BuildConfig.SERVER_DOMAIN + BuildConfig.SERVER_API_BASE
    }
}

