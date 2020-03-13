package com.arjanvlek.oxygenupdater.apis

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Used for downloading update ZIPs from OnePlus OTA servers
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
interface DownloadApi {

    @Streaming
    @GET
    suspend fun downloadZip(
        @Url url: String,
        @Header("Range") rangeHeader: String? = null
    ): Response<ResponseBody>
}
