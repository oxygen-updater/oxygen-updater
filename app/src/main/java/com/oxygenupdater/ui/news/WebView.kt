/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oxygenupdater.ui.news

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.http.SslError
import android.os.Build
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.oxygenupdater.ui.news.WebViewError.Companion.from
import com.oxygenupdater.utils.AppUserAgent
import android.webkit.WebViewClient as AndroidWebViewClient

/**
 * A wrapper around Android WebView specifically for OU articles.
 *
 * @param state [WebViewState]. Use [rememberWebViewState].
 */
@Composable
fun WebView(
    state: WebViewState,
    modifier: Modifier = Modifier,
) {
    BackHandler(state.canGoBack) { state.webView?.goBack() }

    // AndroidViews are not supported by preview
    if (!LocalInspectionMode.current) AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                // Must be done to avoid the white background in dark themes
                setBackgroundColor(Color.TRANSPARENT)

                with(settings) {
                    @SuppressLint("SetJavaScriptEnabled")
                    javaScriptEnabled = true
                    userAgentString = AppUserAgent + try {
                        " " + WebSettings.getDefaultUserAgent(context)
                    } catch (e: Exception) {
                        ""
                    }
                }

                // Set respective clients, and also update the internal states
                // to ensure they always use the same instance as the parent composable
                webChromeClient = WebChromeClient.apply {
                    this.state = state
                }
                webViewClient = WebViewClient(context).apply {
                    this.state = state
                }
            }.also { state.webView = it }
        },
        onRelease = WebView::destroy,
        modifier = modifier
    )
}

@Immutable
sealed class LoadingState {

    /** Before [WebViewClient.onPageStarted] */
    @Immutable
    data object Initializing : LoadingState()

    /**
     * Between [Initializing] & [Finished]
     *
     * @param progress updated in [WebChromeClient.onProgressChanged]
     */
    @Immutable
    data class Loading(val progress: Float) : LoadingState()

    /** After [WebViewClient.onPageFinished] or [WebViewClient.onPageCommitVisible] */
    @Immutable
    data object Finished : LoadingState()
}

/**
 * A state holder to hold the state for the WebView. In most cases this will be remembered
 * using the rememberWebViewState(uri) function.
 */
@Stable
class WebViewState {

    var loadingState: LoadingState by mutableStateOf(LoadingState.Initializing)

    /**
     * First error captured in the last load; reset when a new page is loaded.
     * Error is guaranteed to be only for our API URLs (via [WebViewClient.updateError]).
     */
    var errorForCurrentRequest by mutableStateOf<WebViewError?>(null)

    /** Updated in [WebViewClient.doUpdateVisitedHistory] */
    var canGoBack by mutableStateOf(false)

    /** Access to the [WebView] itself */
    var webView by mutableStateOf<WebView?>(null)
}

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun rememberWebViewState() = remember { WebViewState() }

/**
 * Wrapper around [WebResourceError], [SslError], and [WebResourceResponse].
 *
 * - The constructor is used to create an instance of this class with info
 *   from the deprecated [WebViewClient.onReceivedError] method.
 *   Otherwise, on API 23 (Marshmallow) and above, the [from] method is used.
 * - An instance of this class is created using the 3 respective `from` methods
 *   (one each for each wrapper type)
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Immutable
data class WebViewError(
    val errorCode: Int = AndroidWebViewClient.ERROR_UNKNOWN,
    val description: String? = null,
) {

    /**
     * Error codes have been taken from different classes:
     * * Basic errors: `ERROR_*` in [WebViewClient]
     * * SSL errors: `SSL_*` in [SslError]
     *
     * If none of the codes match, we assume it's a standard HTTP error,
     * and construct the string accordingly (e.g. `ERROR_404`)
     *
     * @see WebResourceError
     * @see SslError
     * @see WebResourceResponse
     */
    val errorCodeString = description ?: when (errorCode) {
        // Basic errors
        AndroidWebViewClient.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
        AndroidWebViewClient.ERROR_HOST_LOOKUP -> "ERROR_HOST_LOOKUP"
        AndroidWebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME -> "ERROR_UNSUPPORTED_AUTH_SCHEME"
        AndroidWebViewClient.ERROR_AUTHENTICATION -> "ERROR_AUTHENTICATION"
        AndroidWebViewClient.ERROR_PROXY_AUTHENTICATION -> "ERROR_PROXY_AUTHENTICATION"
        AndroidWebViewClient.ERROR_CONNECT -> "ERROR_CONNECT"
        AndroidWebViewClient.ERROR_IO -> "ERROR_IO"
        AndroidWebViewClient.ERROR_TIMEOUT -> "ERROR_TIMEOUT"
        AndroidWebViewClient.ERROR_REDIRECT_LOOP -> "ERROR_REDIRECT_LOOP"
        AndroidWebViewClient.ERROR_UNSUPPORTED_SCHEME -> "ERROR_UNSUPPORTED_SCHEME"
        AndroidWebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "ERROR_FAILED_SSL_HANDSHAKE"
        AndroidWebViewClient.ERROR_BAD_URL -> "ERROR_BAD_URL"
        AndroidWebViewClient.ERROR_FILE -> "ERROR_FILE"
        AndroidWebViewClient.ERROR_FILE_NOT_FOUND -> "ERROR_FILE_NOT_FOUND"
        AndroidWebViewClient.ERROR_TOO_MANY_REQUESTS -> "ERROR_TOO_MANY_REQUESTS"
        AndroidWebViewClient.ERROR_UNSAFE_RESOURCE -> "ERROR_UNSAFE_RESOURCE"
        // SSL errors, in order of severity
        SslError.SSL_NOTYETVALID -> "SSL_NOT_YET_VALID"
        SslError.SSL_EXPIRED -> "SSL_EXPIRED"
        SslError.SSL_IDMISMATCH -> "SSL_ID_MISMATCH"
        SslError.SSL_UNTRUSTED -> "SSL_UNTRUSTED"
        SslError.SSL_DATE_INVALID -> "SSL_DATE_INVALID"
        SslError.SSL_INVALID -> "SSL_INVALID"
        // Assume it's an HTTP error
        else -> "ERROR_$errorCode"
    }

    companion object {

        /**
         * API is restricted because the non-deprecated [WebViewClient.onReceivedError]
         * callback was added in API 23 (Marshmallow)
         */
        @RequiresApi(Build.VERSION_CODES.M)
        fun from(resourceError: WebResourceError?) = WebViewError(
            resourceError?.errorCode ?: AndroidWebViewClient.ERROR_UNKNOWN,
            resourceError?.description as? String ?: "ERROR_UNKNOWN",
        )

        fun from(sslError: SslError?) = WebViewError(
            sslError?.primaryError ?: AndroidWebViewClient.ERROR_UNKNOWN,
        )

        /**
         * API is restricted because the [WebViewClient.onReceivedHttpError] callback
         * was added in API 23 (Marshmallow).
         *
         * The callback is called only for status codes >= 400.
         */
        @RequiresApi(Build.VERSION_CODES.M)
        fun from(errorResponse: WebResourceResponse?) = WebViewError(
            errorResponse?.statusCode ?: AndroidWebViewClient.ERROR_UNKNOWN,
        )
    }
}
