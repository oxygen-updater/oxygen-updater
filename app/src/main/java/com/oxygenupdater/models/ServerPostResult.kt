package com.oxygenupdater.models

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class ServerPostResult(
    val success: Boolean = false,

    /** Only used for [com.oxygenupdater.apis.ServerApi.getFreshUpdateDataDownloadUrl] */
    val result: String? = null,

    @Json(name = "error_message")
    val errorMessage: String? = null,
)
