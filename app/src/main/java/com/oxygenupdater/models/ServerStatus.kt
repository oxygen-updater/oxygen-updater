package com.oxygenupdater.models

import androidx.compose.runtime.Immutable
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.internal.ForceBoolean
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class ServerStatus(
    val status: Status? = null,

    @Json(name = "latest_app_version")
    val latestAppVersion: String? = null,

    @Json(name = "automatic_installation_enabled")
    @ForceBoolean val automaticInstallationEnabled: Boolean = false,

    @Json(name = "push_notification_delay_seconds")
    val pushNotificationDelaySeconds: Int = 0,
) {

    fun checkIfAppIsUpToDate() = try {
        appVersionNumeric >= latestAppVersion!!.replace(".", "").toInt()
    } catch (e: Exception) {
        true
    }

    @JsonClass(generateAdapter = false) // https://github.com/square/moshi#enums
    enum class Status {
        NORMAL,
        WARNING,
        ERROR,
        MAINTENANCE,
        OUTDATED,
        UNREACHABLE;

        val isUserRecoverableError
            get() = equals(WARNING) || equals(ERROR) || equals(UNREACHABLE)

        val isNonRecoverableError
            get() = !isUserRecoverableError && !equals(NORMAL)
    }

    companion object {
        private val appVersionNumeric = BuildConfig.VERSION_NAME.replace(".", "")
            // handle custom buildConfigs
            .split("-")[0].toInt()
    }
}
