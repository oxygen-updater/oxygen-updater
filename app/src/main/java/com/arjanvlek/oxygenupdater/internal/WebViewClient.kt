package com.arjanvlek.oxygenupdater.internal

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
import com.arjanvlek.oxygenupdater.utils.Logger.logDebug

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
     */
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ) = request?.let {
        context.startActivity(Intent(Intent.ACTION_VIEW, request.url)).let { true }
    } ?: super.shouldOverrideUrlLoading(view, request)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        resourceError: WebResourceError?
    ) = super.onReceivedError(view, request, resourceError).also {
        val error = WebViewError.from(resourceError)

        logDebug(TAG, "Received error: $error")
        updateError(error)
    }

    @Suppress("DEPRECATION")
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) = super.onReceivedError(view, errorCode, description, failingUrl).also {
        val error = WebViewError(errorCode, description)

        logDebug(TAG, "Received error: $error")
        updateError(error)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) = super.onReceivedHttpError(view, request, errorResponse).also {
        val error = WebViewError.from(errorResponse)

        logDebug(TAG, "Received HTTP error: $error")
        updateError(error)
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        sslError: SslError?
    ) = super.onReceivedSslError(view, handler, sslError).also {
        val error = WebViewError.from(sslError)

        logDebug(TAG, "Received SSL error: $error")
        updateError(error)
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
     * Set [error] only if it hasn't been set already
     */
    private fun updateError(error: WebViewError) {
        if (this.error == null) {
            this.error = error
        }
    }

    companion object {
        private const val TAG = "WebViewClient"
    }
}

