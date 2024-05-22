package com.oxygenupdater.apis

import com.oxygenupdater.models.Article
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.InAppFaq
import com.oxygenupdater.models.InstallGuide
import com.oxygenupdater.models.ServerMessage
import com.oxygenupdater.models.ServerPostResult
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.ui.currentLanguage
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.utils.HeaderReadTimeout
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Used to communicate with the Oxygen Updater backend */
interface ServerApi {

    @GET("flattenedFaq")
    suspend fun fetchFaq(
        @Query("language") language: String = currentLanguage,
    ): Response<List<InAppFaq>>

    @GET("installGuide")
    suspend fun fetchInstallGuide(
        @Query("language") language: String = currentLanguage,
    ): Response<List<InstallGuide>>

    @GET("devices/{filter}")
    suspend fun fetchDevices(@Path("filter") filter: String): Response<List<Device>>

    @GET("updateData/{deviceId}/{updateMethodId}/{incrementalSystemVersion}")
    suspend fun fetchUpdateData(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Path("incrementalSystemVersion") incrementalSystemVersion: String,
        @Query("osVersion") osVersion: String,
        @Query("osType") osType: String,
        @Query("fingerprint") fingerprint: String,
        @Query("isEuBuild") isEuBuild: Boolean,
        @Query("appVersion") appVersion: String,
    ): Response<UpdateData>

    @GET("mostRecentUpdateData/{deviceId}/{updateMethodId}")
    suspend fun fetchMostRecentUpdateData(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
    ): Response<UpdateData>

    @GET("serverStatus")
    suspend fun fetchServerStatus(): Response<ServerStatus>

    @GET("serverMessages/{deviceId}/{updateMethodId}")
    suspend fun fetchServerMessages(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Query("language") language: String = currentLanguage,
    ): Response<List<ServerMessage>>

    @GET("updateMethods/{deviceId}")
    suspend fun fetchUpdateMethodsForDevice(
        @Path("deviceId") deviceId: Long,
        @Query("language") language: String = currentLanguage,
    ): Response<List<UpdateMethod>>

    @GET("news/{deviceId}/{updateMethodId}")
    suspend fun fetchNews(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Query("language") language: String = if (currentLocale.language == "nl") "nl" else "en",
    ): Response<List<Article>>

    @GET("news-item/{id}")
    suspend fun fetchArticle(
        @Path("id") id: Long,
        @Query("language") language: String = if (currentLocale.language == "nl") "nl" else "en",
    ): Response<Article>

    @POST("news-read")
    suspend fun markArticleRead(@Body id: Map<String, Long>): Response<ServerPostResult>

    @POST("osInfoHeartbeat")
    suspend fun osInfoHeartbeat(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ServerPostResult>

    /**
     * @param body includes the following fields:
     * * `rows: List<Map<String, Any?>>`,
     * * `otaVersion: String`,
     * * `isEuBuild: Boolean`,
     * * `appVersion: String`,
     * * `deviceName: String`,
     * * `actualDeviceName: String`,
     */
    @POST("submit-update-url")
    suspend fun submitOtaDbRows(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ServerPostResult>

    /**
     * @param body includes the following fields:
     * * `url: String`
     * * `otaVersion: String`,
     * * `httpCode: Int`,
     * * `httpMessage: String`,
     * * `appVersion: String`,
     * * `deviceName: String`
     */
    @POST("log-download-error")
    suspend fun logDownloadError(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<ServerPostResult>

    @POST("verify-purchase")
    @Headers("$HeaderReadTimeout:99")
    suspend fun verifyPurchase(@Body purchaseData: Map<String, @JvmSuppressWildcards Any?>): Response<ServerPostResult>
}
