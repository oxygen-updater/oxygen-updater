package com.arjanvlek.oxygenupdater.apis

import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.InstallGuidePage
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerMessage
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.models.ServerStatus
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.models.UpdateMethod
import com.arjanvlek.oxygenupdater.utils.HEADER_READ_TIMEOUT
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Used to communicate with the Oxygen Updater backend
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
interface ServerApi {

    @GET("devices/{filter}")
    suspend fun fetchDevices(
        @Path("filter") filter: String
    ): Response<List<Device>>

    @GET("updateData/{deviceId}/{updateMethodId}/{incrementalSystemVersion}")
    suspend fun fetchUpdateData(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Path("incrementalSystemVersion") incrementalSystemVersion: String
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
     * * `isEuBuild: Boolean`
     */
    @POST("submit-update-file")
    suspend fun submitUpdateFile(
        @Body body: HashMap<String, Any>
    ): Response<ServerPostResult>

    @POST("log-update-installation")
    suspend fun logRootInstall(
        @Body rootInstall: RootInstall
    ): Response<ServerPostResult>

    @POST("verify-purchase")
    @Headers("$HEADER_READ_TIMEOUT:120")
    suspend fun verifyPurchase(
        @Body purchaseData: HashMap<String, Any?>
    ): Response<ServerPostResult>
}
