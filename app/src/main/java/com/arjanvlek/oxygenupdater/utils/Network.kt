package com.arjanvlek.oxygenupdater.utils

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.exceptions.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.objectMapper
import com.arjanvlek.oxygenupdater.utils.Logger.logVerbose
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */

private const val TAG = "OxygenUpdaterNetwork"
private const val USER_AGENT_TAG = "User-Agent"
private const val APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME
private const val CACHE_SIZE = 10L * 1024 * 1024 // 10 MB

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
        getCacheSizeBytes(getUuidForPath(context.cacheDir))
    }
} else {
    CACHE_SIZE
}.let { cacheSize ->
    Cache(context.cacheDir, cacheSize)
}

private fun httpClient(cache: Cache) = OkHttpClient.Builder().apply {
    cache(cache)
    addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader(USER_AGENT_TAG, APP_USER_AGENT)
            .build()

        logVerbose(TAG, "Performing ${request.method} request to URL ${request.url}")

        if (request.url.pathSegments.last() == "verify-purchase") {
            logVerbose(TAG, "Timeout is set to 120 seconds.")
            chain.withReadTimeout(120, TimeUnit.SECONDS).proceed(request)
        } else {
            logVerbose(TAG, "Timeout is set to 10 seconds.")
            chain.proceed(request)
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

fun <T> apiResponse(retrofitResponse: Response<T>): T {
    if (retrofitResponse.isSuccessful && retrofitResponse.code() in 200..299) {
        retrofitResponse.body()!!.let {
            logVerbose(TAG, "Response: $it")
            return it!!
        }
    } else if (retrofitResponse.isSuccessful) {
        logWarning(TAG, "Response: ${retrofitResponse.body()}", OxygenUpdaterException("API Response Error: ${retrofitResponse.code()}"))
        throw OxygenUpdaterException("API Response Error: ${retrofitResponse.code()}")
    } else {
        val json = retrofitResponse.errorBody()?.string()
        logWarning(TAG, "Response: $json", OxygenUpdaterException("API Response Error: $json"))
        throw OxygenUpdaterException("API Response Error: $json")
    }
}
