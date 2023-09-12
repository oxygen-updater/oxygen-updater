package com.oxygenupdater.models

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class ServerPostResult(
    val success: Boolean = false,

    @Json(name = "error_message")
    val errorMessage: String? = null,
)
