package com.arjanvlek.oxygenupdater.models

import android.content.Context
import androidx.core.content.ContextCompat
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.Utils
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

    override fun getBannerText(context: Context?): String? {
        return when (status) {
            Status.WARNING -> context!!.getString(R.string.server_status_warning)
            Status.ERROR -> context!!.getString(R.string.server_status_error)
            Status.MAINTENANCE -> ""
            Status.OUTDATED -> ""
            Status.UNREACHABLE -> context!!.getString(R.string.server_status_unreachable)
            else -> ""
        }
    }

    override fun getColor(context: Context?): Int {
        return when (status) {
            Status.WARNING -> ContextCompat.getColor(context!!, R.color.colorWarn)
            Status.ERROR -> ContextCompat.getColor(context!!, R.color.colorPrimary)
            Status.MAINTENANCE -> 0
            Status.OUTDATED -> 0
            Status.UNREACHABLE -> ContextCompat.getColor(context!!, R.color.colorPrimary)
            else -> 0
        }
    }

    fun checkIfAppIsUpToDate(): Boolean {
        return try {
            val appVersionNumeric = BuildConfig.VERSION_NAME.replace(".", "").toInt()
            val appVersionFromResultNumeric = latestAppVersion!!.replace(".", "").toInt()
            appVersionFromResultNumeric <= appVersionNumeric
        } catch (e: Exception) {
            true
        }
    }

    enum class Status {
        NORMAL,
        WARNING,
        ERROR,
        MAINTENANCE,
        OUTDATED,
        UNREACHABLE;

        val isUserRecoverableError: Boolean
            get() = equals(WARNING) || equals(ERROR) || equals(UNREACHABLE)

        val isNonRecoverableError: Boolean
            get() = !isUserRecoverableError && !equals(NORMAL)
    }
}
