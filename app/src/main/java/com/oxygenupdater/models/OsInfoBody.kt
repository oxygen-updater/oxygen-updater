package com.oxygenupdater.models

import com.oxygenupdater.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
class OsInfoBody private constructor(
    val fromAction: String,
    val otaVersion: String,
    val osVersion: String,
    val osType: String,
    val fingerprint: String,
    val oplusPipeline: String,
    val oplusPipelineCode: String,
    val oplusManifestHash: String,
    val deviceMarketName: String,
    val isEuBuild: Boolean,
    val appVersion: String,
) {
    companion object {
        fun from(fromIntentAction: String) = OsInfoBody(
            fromAction = fromIntentAction,
            otaVersion = SystemVersionProperties.otaVersion,
            osVersion = SystemVersionProperties.osVersion,
            osType = SystemVersionProperties.osType,
            fingerprint = SystemVersionProperties.fingerprint,
            oplusPipeline = SystemVersionProperties.pipeline,
            oplusPipelineCode = SystemVersionProperties.pipelineCode,
            oplusManifestHash = SystemVersionProperties.manifestHash,
            deviceMarketName = SystemVersionProperties.deviceMarketName,
            isEuBuild = SystemVersionProperties.isEuBuild,
            appVersion = BuildConfig.VERSION_NAME,
        )
    }
}
