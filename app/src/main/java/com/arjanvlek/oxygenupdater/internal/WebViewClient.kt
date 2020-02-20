package com.arjanvlek.oxygenupdater.internal

import android.content.Context
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Needed for handling page load finish events.
 * Note: Overriding [shouldOverrideUrlLoading] is necessary, as without it, custom URL schemes won't work (mailto, tel, etc)
 * Moreover, clicking page links in the associated WebView will load those pages within the WebView, which is not desired.
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class WebViewClient(
    private val context: Context,
    private val pageCommitVisibleCallback: () -> Unit
) : WebViewClient() {

    /**
     * [Intent.ACTION_VIEW] should be enough to handle cases of opening page links in the browser,
     * as well as handle custom URL schemes (apps register themselves to handle URIs: e.g. `mailto://`, `reddit://`, `tel://`, etc)
     */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = request?.let {
        context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
        true
    } ?: super.shouldOverrideUrlLoading(view, request)

    /**
     * Just invokes the callback, nothing special
     */
    override fun onPageCommitVisible(view: WebView?, url: String?) = pageCommitVisibleCallback.invoke().also {
        super.onPageCommitVisible(view, url)
    }
}

