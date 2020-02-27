package com.arjanvlek.oxygenupdater.utils

import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.exceptions.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.objectMapper
import com.arjanvlek.oxygenupdater.utils.Logger.logVerbose
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */

private const val TAG = "OxygenUpdaterNetwork"
private const val USER_AGENT_TAG = "User-Agent"
private const val APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME

fun createNetworkClient(baseUrl: String) = retrofitClient(baseUrl, httpClient())

private fun httpClient() = OkHttpClient.Builder()
    .addInterceptor { chain ->
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
    .build()

private fun retrofitClient(baseUrl: String, httpClient: OkHttpClient) = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(httpClient)
    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
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
