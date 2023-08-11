package com.oxygenupdater.models

import androidx.compose.runtime.Immutable

@Immutable
data class ServerPostResult(
    val success: Boolean = false,
    val errorMessage: String? = null,
)
