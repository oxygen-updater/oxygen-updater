package com.oxygenupdater.models

import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.oxygenupdater.BuildConfig

@JsonIgnoreProperties(ignoreUnknown = true)
@Immutable
data class ServerStatus(
    val status: Status? = null,
    val latestAppVersion: String? = null,
    val automaticInstallationEnabled: Boolean = false,
    val pushNotificationDelaySeconds: Int = 0,
) {

    fun checkIfAppIsUpToDate() = try {
        appVersionNumeric >= latestAppVersion!!.replace(".", "").toInt()
    } catch (e: Exception) {
        true
    }

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
