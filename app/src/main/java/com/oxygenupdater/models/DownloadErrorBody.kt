package com.oxygenupdater.models

import kotlinx.serialization.Serializable

@Serializable
class DownloadErrorBody(
    val url: String?,
    val filename: String?,
    val version: String?,
    val otaVersion: String?,
    val httpCode: Int,
    val httpMessage: String?,
    val appVersion: String,
    val deviceName: String,
    val actualDeviceName: String,
)
