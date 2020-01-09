package com.arjanvlek.oxygenupdater.internal.server

import com.fasterxml.jackson.annotation.JsonProperty

data class ServerPostResult(
    val success: Boolean = false,

    @JsonProperty("error_message") val errorMessage: String? = null
)
