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

    /**
     * @param currentVersionName parameter exists only to help with testing
     *
     * @return `true` if [current app version][currentVersionName] is older than server-provided [latestAppVersion]
     */
    fun shouldShowAppUpdateNotice(
        currentVersionName: String = BuildConfig.VERSION_NAME,
    ) = if (latestAppVersion == currentVersionName) false else try {
        val currentSemverInts = currentVersionName.toSemverInts()
        val latestSemverInts = latestAppVersion!!.toSemverInts()
        repeat(3) { // limit to 3; our versions are strictly `major.minor.patch`
            val result = currentSemverInts.getOrElse(it) { 0 } compareTo latestSemverInts.getOrElse(it) { 0 }
            // We're running through major -> minor -> patch, so if we have
            // anything that's not equal, we already have our final result.
            if (result != 0) return result <= 0 // current <= latest
        }
        false // current == latest
    } catch (e: Exception) {
        false
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
        private inline fun String.toSemverInts() =
            split("-")[0] // handle custom builds (e.g. debug, benchmark, etc)
                .split(".").map { it.toInt() } // map each component to int
    }
}
