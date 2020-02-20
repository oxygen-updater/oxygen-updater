package com.arjanvlek.oxygenupdater.models

import android.content.Context
import androidx.core.content.ContextCompat
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.utils.Utils
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServerStatus(
    var status: Status? = null,
    var latestAppVersion: String? = null,
    var automaticInstallationEnabled: Boolean = false,
    var pushNotificationDelaySeconds: Int = 0
) : Banner {

    @JsonProperty("push_notification_delay_seconds")
    fun setPushNotificationDelaySeconds(pushNotificationDelaySeconds: String?) {
        if (pushNotificationDelaySeconds != null && Utils.isNumeric(pushNotificationDelaySeconds)) {
            this.pushNotificationDelaySeconds = pushNotificationDelaySeconds.toInt()
        }
    }

    override fun getBannerText(context: Context) = when (status) {
        Status.WARNING -> context.getString(R.string.server_status_warning)
        Status.ERROR -> context.getString(R.string.server_status_error)
        Status.MAINTENANCE -> ""
        Status.OUTDATED -> ""
        Status.UNREACHABLE -> context.getString(R.string.server_status_unreachable)
        else -> ""
    }

    override fun getColor(context: Context) = when (status) {
        Status.WARNING -> ContextCompat.getColor(context, R.color.colorWarn)
        Status.ERROR -> ContextCompat.getColor(context, R.color.colorPrimary)
        Status.MAINTENANCE -> 0
        Status.OUTDATED -> 0
        Status.UNREACHABLE -> ContextCompat.getColor(context, R.color.colorPrimary)
        else -> 0
    }

    override fun getDrawableRes(context: Context) = when (status) {
        Status.WARNING -> R.drawable.warning
        Status.ERROR -> R.drawable.error_outline
        Status.MAINTENANCE -> 0
        Status.OUTDATED -> 0
        Status.UNREACHABLE -> R.drawable.info
        else -> 0
    }

    fun checkIfAppIsUpToDate() = try {
        val appVersionNumeric = BuildConfig.VERSION_NAME.replace(".", "")
            // handle custom buildConfigs
            .split("-")[0]
            .toInt()
        val appVersionFromResultNumeric = latestAppVersion!!.replace(".", "").toInt()
        appVersionFromResultNumeric <= appVersionNumeric
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
}
