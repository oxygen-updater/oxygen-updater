package com.oxygenupdater.ui.news

import android.webkit.WebView
import com.oxygenupdater.utils.logDebug

/**
 * Wrapper around [android.webkit.WebChromeClient] with one responsibility:
 * - Set [WebViewState.loadingState] to [LoadingState.Loading] with the
 *   correct `progress` fraction.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
object WebChromeClient : android.webkit.WebChromeClient() {

    private const val TAG = "WebChromeClient"

    lateinit var state: WebViewState

    override fun onProgressChanged(
        view: WebView,
        newProgress: Int,
    ) = super.onProgressChanged(view, newProgress).also {
        if (state.loadingState is LoadingState.Finished) return
        logDebug(TAG, "Progress changed: $newProgress")
        state.loadingState = LoadingState.Loading(newProgress / 100f)
    }
}
