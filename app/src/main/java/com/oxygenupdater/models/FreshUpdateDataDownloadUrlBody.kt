package com.oxygenupdater.models

import kotlinx.serialization.Serializable

@Serializable
class FreshUpdateDataDownloadUrlBody(
    val id: Long?,
    val otaVersionNumber: String?,
    val downloadUrl: String?,
    val md5sum: String?,
)
