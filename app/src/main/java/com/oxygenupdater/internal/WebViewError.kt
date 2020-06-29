package com.oxygenupdater.internal

import android.net.http.SslError
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.oxygenupdater.internal.WebViewError.Companion.from

/**
 * Wrapper around [WebResourceError], [SslError], and [WebResourceResponse].
 *
 * * The constructor is used to create an instance of this class with info from the deprecated [WebViewClient.onReceivedError] method.
 *   Otherwise, on API 23 (Marshmallow) and above, the [from] method is used
 * * An instance of this class is created using the 3 respective `from` methods (one each for each wrapper type)
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
data class WebViewError(
    val errorCode: Int = WebViewClient.ERROR_UNKNOWN,
    val description: CharSequence? = null
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
        WebViewClient.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
        WebViewClient.ERROR_HOST_LOOKUP -> "ERROR_HOST_LOOKUP"
        WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME -> "ERROR_UNSUPPORTED_AUTH_SCHEME"
        WebViewClient.ERROR_AUTHENTICATION -> "ERROR_AUTHENTICATION"
        WebViewClient.ERROR_PROXY_AUTHENTICATION -> "ERROR_PROXY_AUTHENTICATION"
        WebViewClient.ERROR_CONNECT -> "ERROR_CONNECT"
        WebViewClient.ERROR_IO -> "ERROR_IO"
        WebViewClient.ERROR_TIMEOUT -> "ERROR_TIMEOUT"
        WebViewClient.ERROR_REDIRECT_LOOP -> "ERROR_REDIRECT_LOOP"
        WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "ERROR_UNSUPPORTED_SCHEME"
        WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "ERROR_FAILED_SSL_HANDSHAKE"
        WebViewClient.ERROR_BAD_URL -> "ERROR_BAD_URL"
        WebViewClient.ERROR_FILE -> "ERROR_FILE"
        WebViewClient.ERROR_FILE_NOT_FOUND -> "ERROR_FILE_NOT_FOUND"
        WebViewClient.ERROR_TOO_MANY_REQUESTS -> "ERROR_TOO_MANY_REQUESTS"
        WebViewClient.ERROR_UNSAFE_RESOURCE -> "ERROR_UNSAFE_RESOURCE"
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
         * API is restricted because the non-deprecated [WebViewClient.onReceivedError] callback was added in API 23 (Marshmallow)
         */
        @RequiresApi(Build.VERSION_CODES.M)
        fun from(resourceError: WebResourceError?) = WebViewError(
            resourceError?.errorCode ?: WebViewClient.ERROR_UNKNOWN,
            resourceError?.description
        )

        fun from(sslError: SslError?) = WebViewError(
            sslError?.primaryError ?: WebViewClient.ERROR_UNKNOWN
        )

        /**
         * API is restricted because the [WebViewClient.onReceivedHttpError] callback was added in API 23 (Marshmallow).
         *
         * The callback is called only for status codes >= 400
         */
        @RequiresApi(Build.VERSION_CODES.M)
        fun from(errorResponse: WebResourceResponse?) = WebViewError(
            errorResponse?.statusCode ?: WebViewClient.ERROR_UNKNOWN
        )
    }
}
