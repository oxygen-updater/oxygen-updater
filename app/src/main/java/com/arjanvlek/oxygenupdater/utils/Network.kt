package com.arjanvlek.oxygenupdater.utils

import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.internal.objectMapper
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */

private const val USER_AGENT_TAG = "User-Agent"
private const val APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME

fun createNetworkClient(baseUrl: String) = retrofitClient(baseUrl, httpClient())

private fun httpClient() = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader(USER_AGENT_TAG, APP_USER_AGENT)
            .build()

        if (request.url.pathSegments.last() == "verify-purchase") {
            chain.withReadTimeout(120, TimeUnit.SECONDS).proceed(request)
        } else {
            chain.proceed(request)
        }
    }
    .build()

private fun retrofitClient(baseUrl: String, httpClient: OkHttpClient) = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(httpClient)
    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
    .build()

fun <T> apiResponse(retrofitResponse: Response<T>): T {
    if (retrofitResponse.isSuccessful && retrofitResponse.code() in 200..299) {
        return retrofitResponse.body()!!
    } else if (retrofitResponse.isSuccessful) {
        throw Exception("API Response Error: ${retrofitResponse.code()}")
    } else {
        val json = retrofitResponse.errorBody()?.string()
        throw Exception("API Response Error: $json")
    }
}
