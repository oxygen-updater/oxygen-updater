package com.oxygenupdater.utils

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.exceptions.OxygenUpdaterException
import com.oxygenupdater.internal.objectMapper
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logVerbose
import com.oxygenupdater.utils.Logger.logWarning
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */

private const val TAG = "OxygenUpdaterNetwork"
private const val USER_AGENT_TAG = "User-Agent"
private const val APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME
private const val CACHE_SIZE = 10L * 1024 * 1024 // 10 MB

const val HEADER_READ_TIMEOUT = "X-Read-Timeout"

fun createNetworkClient(cache: Cache) = retrofitClient(httpClient(cache))
fun createDownloadClient() = retrofitClientForDownload(httpClientForDownload())

/**
 * Create a [Cache] for [OkHttpClient].
 *
 * Limit cache size to stay under quota if device is Oreo and above.
 * Otherwise, default to [CACHE_SIZE]
 */
fun createOkHttpCache(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.getSystemService<StorageManager>()!!.run {
        try {
            getCacheQuotaBytes(getUuidForPath(context.cacheDir)).let {
                if (it != 0L) it
                else CACHE_SIZE
            }
        } catch (e: IOException) {
            CACHE_SIZE
        }
    }
} else {
    CACHE_SIZE
}.let { cacheSize ->
    Cache(context.cacheDir, cacheSize)
}

private fun httpClient(cache: Cache) = OkHttpClient.Builder().apply {
    cache(cache)
    addInterceptor { chain ->
        val request = chain.request()

        logDebug(TAG, "Method: ${request.method}, URL: ${request.url}")

        val builder = request.newBuilder()
            .addHeader(USER_AGENT_TAG, APP_USER_AGENT)

        val readTimeout = request.header(HEADER_READ_TIMEOUT)?.toInt()?.let {
            builder.removeHeader(HEADER_READ_TIMEOUT)
            it
        }

        chain.run {
            if (readTimeout != null) {
                logDebug(TAG, "readTimeout = ${readTimeout}s")

                withReadTimeout(readTimeout, TimeUnit.SECONDS)
            } else {
                logDebug(TAG, "readTimeout = ${chain.readTimeoutMillis() / 1000}s")
            }

            proceed(builder.build())
        }
    }

    if (BuildConfig.DEBUG) {
        addInterceptor(HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BASIC)
        })
    }
}.build()

private fun httpClientForDownload() = OkHttpClient.Builder().apply {
    if (BuildConfig.DEBUG) {
        addInterceptor(HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BASIC)
        })
    }
}.build()

private fun retrofitClient(httpClient: OkHttpClient) = Retrofit.Builder()
    .baseUrl(BuildConfig.SERVER_BASE_URL)
    .client(httpClient)
    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
    .build()

private fun retrofitClientForDownload(httpClient: OkHttpClient) = Retrofit.Builder()
    .baseUrl(BuildConfig.SERVER_BASE_URL)
    .client(httpClient)
    .build()

suspend inline fun <reified R> performServerRequest(
    crossinline block: suspend () -> Response<R>
): R? {
    val logTag = "OxygenUpdaterNetwork"

    var retryCount = 0
    while (retryCount < 5) {
        try {
            val response = block()

            return if (response.isSuccessful) {
                response.body().apply {
                    logVerbose(logTag, "Response: $this")
                }
            } else {
                val json = convertErrorBody(response)
                logWarning(
                    logTag, "Response: $json",
                    OxygenUpdaterException("API Response Error: $json")
                )
                null
            }
        } catch (e: Exception) {
            when {
                e is HttpException -> logWarning(
                    logTag,
                    "HttpException: [code: ${e.code()}, errorBody: ${convertErrorBody(e)}]"
                )
                ExceptionUtils.isNetworkError(e) -> logWarning(
                    logTag,
                    "Network error while performing request", e
                )
                else -> logError(
                    logTag,
                    "Error performing request", e
                )
            }

            if (retryCount++ < 5) {
                logDebug(logTag, "Retrying the request ($retryCount/5)")
            }
        }
    }

    return null
}

fun <T> convertErrorBody(response: Response<T>) = try {
    response.errorBody()?.string()
} catch (exception: Exception) {
    null
}

fun convertErrorBody(e: HttpException) = try {
    e.response()?.errorBody()?.string()
} catch (exception: Exception) {
    null
}
