package com.oxygenupdater.models

import com.oxygenupdater.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
class SubmitOtaDbBody private constructor(
    val rows: List<Map<String, String?>>,
    val fingerprint: String,
    val currentOtaVersion: String,
    val isEuBuild: Boolean,
    val appVersion: String,
    val deviceName: String,
    val actualDeviceName: String,
) {
    companion object {
        fun from(rows: List<Map<String, String?>>, deviceName: String) = SubmitOtaDbBody(
            rows = rows,
            fingerprint = SystemVersionProperties.fingerprint,
            currentOtaVersion = SystemVersionProperties.otaVersion,
            isEuBuild = SystemVersionProperties.isEuBuild,
            appVersion = BuildConfig.VERSION_NAME,
            deviceName = deviceName,
            actualDeviceName = SystemVersionProperties.deviceProductName,
        )
    }
}
