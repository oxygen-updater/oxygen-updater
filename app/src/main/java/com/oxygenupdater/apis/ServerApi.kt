package com.oxygenupdater.apis

import com.oxygenupdater.models.Device
import com.oxygenupdater.models.InAppFaq
import com.oxygenupdater.models.InstallGuidePage
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.ServerMessage
import com.oxygenupdater.models.ServerPostResult
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.utils.HEADER_READ_TIMEOUT
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Used to communicate with the Oxygen Updater backend
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
interface ServerApi {

    @GET("flattenedFaq")
    suspend fun fetchFaq(): Response<List<InAppFaq>>

    @GET("devices/{filter}")
    suspend fun fetchDevices(
        @Path("filter") filter: String
    ): Response<List<Device>>

    @GET("updateData/{deviceId}/{updateMethodId}/{incrementalSystemVersion}")
    suspend fun fetchUpdateData(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Path("incrementalSystemVersion") incrementalSystemVersion: String,
        @Query("osVersion") osVersion: String,
        @Query("osType") osType: String,
        @Query("fingerprint") fingerprint: String,
        @Query("isEuBuild") isEuBuild: Boolean,
        @Query("appVersion") appVersion: String
    ): Response<UpdateData>

    @GET("mostRecentUpdateData/{deviceId}/{updateMethodId}")
    suspend fun fetchMostRecentUpdateData(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long
    ): Response<UpdateData>

    @GET("serverStatus")
    suspend fun fetchServerStatus(): Response<ServerStatus>

    @GET("serverMessages/{deviceId}/{updateMethodId}")
    suspend fun fetchServerMessages(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long
    ): Response<List<ServerMessage>>

    @GET("news/{deviceId}/{updateMethodId}")
    suspend fun fetchNews(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long
    ): Response<List<NewsItem>>

    @GET("news-item/{newsItemId}")
    suspend fun fetchNewsItem(
        @Path("newsItemId") newsItemId: Long
    ): Response<NewsItem>

    @POST("news-read")
    suspend fun markNewsItemRead(
        @Body newsItemId: Map<String, Long>
    ): Response<ServerPostResult>

    @GET("updateMethods/{deviceId}")
    suspend fun fetchUpdateMethodsForDevice(
        @Path("deviceId") deviceId: Long
    ): Response<List<UpdateMethod>>

    @GET("allUpdateMethods")
    suspend fun fetchAllUpdateMethods(): Response<List<UpdateMethod>>

    @GET("installGuide/{deviceId}/{updateMethodId}/{pageNumber}")
    suspend fun fetchInstallGuidePage(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Path("pageNumber") pageNumber: Int
    ): Response<InstallGuidePage>

    /**
     * @param body includes the following fields:
     * * `filename: String`
     * * `isEuBuild: Boolean`,
     * * `appVersion: String`,
     * * `deviceName: String`
     */
    @POST("submit-update-file")
    suspend fun submitUpdateFile(
        @Body body: HashMap<String, Any>
    ): Response<ServerPostResult>

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
    suspend fun logDownloadError(
        @Body body: HashMap<String, Any?>
    ): Response<ServerPostResult>

    @POST("verify-purchase")
    @Headers("$HEADER_READ_TIMEOUT:120")
    suspend fun verifyPurchase(
        @Body purchaseData: HashMap<String, Any?>
    ): Response<ServerPostResult>
}
