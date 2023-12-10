package com.oxygenupdater.utils

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.apis.DownloadApi
import com.oxygenupdater.apis.ServerApi
import com.oxygenupdater.internal.BooleanJsonAdapter
import com.oxygenupdater.internal.CsvListJsonAdapter
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit


private const val TAG = "OxygenUpdaterNetwork"
private const val UserAgentTag = "User-Agent"
private const val DefaultCacheSize = 10L * 1024 * 1024 // 10 MB

const val AppUserAgent = "Oxygen_updater_" + BuildConfig.VERSION_NAME
const val ApiBaseUrl = BuildConfig.SERVER_DOMAIN + BuildConfig.SERVER_API_BASE

/** Allows per-request read timeout override via application interceptor */
const val HeaderReadTimeout = "X-Read-Timeout"

fun createServerApi(context: Context) = Retrofit.Builder()
    .baseUrl(ApiBaseUrl)
    .client(httpClient(createOkHttpCache(context)))
    .addConverterFactory(
        MoshiConverterFactory.create(
            Moshi.Builder()
                .add(BooleanJsonAdapter()) // coerce strings/numbers to boolean
                .add(CsvListJsonAdapter())
                .build()
        )
    ).build().create<ServerApi>()

fun createDownloadApi() = Retrofit.Builder()
    .baseUrl(ApiBaseUrl).apply {
        if (BuildConfig.DEBUG) client(
            // Use a network interceptor to log all requests, even intermediary ones like redirects.
            OkHttpClient.Builder().addNetworkInterceptor(HttpLoggingInterceptor().setLevel(Level.BASIC)).build()
        )
    }.build().create<DownloadApi>()

/**
 * Create a [Cache] for [OkHttpClient].
 *
 * Limit cache size to stay under quota if device is Oreo and above.
 * Otherwise, default to [DefaultCacheSize]
 */
private fun createOkHttpCache(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.getSystemService<StorageManager>()?.run {
        try {
            val quota = getCacheQuotaBytes(getUuidForPath(context.cacheDir))
            if (quota > 0L) quota else DefaultCacheSize
        } catch (e: Exception) {
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

    // Use a network interceptor to log all requests, even intermediary ones like redirects.
    // Note that this won't be invoked for cached responses as they short-circuit the network.
    if (BuildConfig.DEBUG) addNetworkInterceptor(HttpLoggingInterceptor().setLevel(Level.BASIC))

    // Use an application interceptor to add a custom user agent
    addInterceptor { chain ->
        val request = chain.request()
        val builder = request.newBuilder().header(UserAgentTag, AppUserAgent)
        val readTimeout = request.header(HeaderReadTimeout)?.let {
            builder.removeHeader(HeaderReadTimeout)
            it.toIntOrNull()
        }

        chain.run {
            if (readTimeout != null) withReadTimeout(readTimeout, TimeUnit.SECONDS).also {
                logDebug(TAG, "custom readTimeout = ${readTimeout}s")
            } else logDebug(TAG, "default readTimeout = ${chain.readTimeoutMillis() / 1000}s")

            proceed(builder.build())
        }
    }
}.build()
