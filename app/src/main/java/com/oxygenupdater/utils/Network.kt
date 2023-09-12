package com.oxygenupdater.utils

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.internal.BooleanJsonAdapter
import com.oxygenupdater.internal.CsvListJsonAdapter
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logVerbose
import com.oxygenupdater.utils.Logger.logWarning
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */

private const val TAG = "OxygenUpdaterNetwork"
private const val UserAgentTag = "User-Agent"
private const val AppUserAgent = "Oxygen_updater_" + BuildConfig.VERSION_NAME
private const val DefaultCacheSize = 10L * 1024 * 1024 // 10 MB
private const val AppBaseUrl = BuildConfig.SERVER_DOMAIN + BuildConfig.SERVER_API_BASE

const val HEADER_READ_TIMEOUT = "X-Read-Timeout"

fun createNetworkClient(cache: Cache) = retrofitClient(httpClient(cache))
fun createDownloadClient() = retrofitClientForDownload(httpClientForDownload())

/**
 * Create a [Cache] for [OkHttpClient].
 *
 * Limit cache size to stay under quota if device is Oreo and above.
 * Otherwise, default to [DefaultCacheSize]
 */
fun createOkHttpCache(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.getSystemService<StorageManager>()?.run {
        try {
            val quota = getCacheQuotaBytes(getUuidForPath(context.cacheDir))
            if (quota > 0L) quota else DefaultCacheSize
        } catch (e: IOException) {
            DefaultCacheSize
        }
    } ?: DefaultCacheSize
} else {
    DefaultCacheSize
}.let {
    Cache(context.cacheDir, it)
}

private fun httpClient(cache: Cache) = OkHttpClient.Builder().apply {
    cache(cache)
    addInterceptor { chain ->
        val request = chain.request()
        val builder = request.newBuilder().header(UserAgentTag, AppUserAgent)
        val readTimeout = request.header(HEADER_READ_TIMEOUT)?.toInt()?.let {
            builder.removeHeader(HEADER_READ_TIMEOUT)
            it
        }

        chain.run {
            if (readTimeout != null) {
                logDebug(TAG, "readTimeout = ${readTimeout}s")

                withReadTimeout(readTimeout, TimeUnit.SECONDS)
            } else logDebug(TAG, "readTimeout = ${chain.readTimeoutMillis() / 1000}s")

            proceed(builder.build())
        }
    }

    if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor().setLevel(Level.BASIC))
}.build()

private fun httpClientForDownload() = OkHttpClient.Builder().apply {
    if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor().setLevel(Level.BASIC))
}.build()

private fun retrofitClient(httpClient: OkHttpClient) = Retrofit.Builder()
    .baseUrl(AppBaseUrl)
    .client(httpClient)
    .addConverterFactory(
        MoshiConverterFactory.create(
            Moshi.Builder()
                .add(BooleanJsonAdapter()) // coerce strings/numbers to boolean
                .add(CsvListJsonAdapter())
                .build()
        )
    )
    .build()

private fun retrofitClientForDownload(httpClient: OkHttpClient) = Retrofit.Builder()
    .baseUrl(AppBaseUrl)
    .client(httpClient)
    .build()

@Suppress("RedundantSuspendModifier")
suspend inline fun <reified R> performServerRequest(block: () -> Response<R>): R? {
    val logTag = "OxygenUpdaterNetwork"

    var retryCount = 0
    while (retryCount < 5) try {
        val response = block()

        return if (response.isSuccessful) response.body().apply {
            logVerbose(logTag, "Response: $this")
        } else {
            logWarning(logTag, "Error response: ${convertErrorBody(response)}")
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

            else -> logError(logTag, "Error performing request", e)
        }

        retryCount++
        logDebug(logTag, "Retrying the request ($retryCount/5)")
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
