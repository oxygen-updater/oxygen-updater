package com.arjanvlek.oxygenupdater.updateinformation

import android.content.Context

import androidx.core.content.ContextCompat

import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.i18n.Locale
import com.fasterxml.jackson.annotation.JsonProperty

class ServerMessage : Banner {
    var id: Long = 0
    @set:JsonProperty("english_message")
    var englishMessage: String? = null
    @set:JsonProperty("dutch_message")
    var dutchMessage: String? = null
    private var deviceId: Long? = null
    private var updateMethodId: Long? = null
    var priority: ServerMessagePriority? = null

    override fun getBannerText(ignored: Context): String? {
        return if (Locale.locale == Locale.NL) dutchMessage else englishMessage
    }

    override fun getColor(context: Context): Int {
        return when (priority) {
            ServerMessagePriority.LOW -> ContextCompat.getColor(context, R.color.colorPositive)
            ServerMessagePriority.MEDIUM -> ContextCompat.getColor(context, R.color.colorWarn)
            ServerMessagePriority.HIGH -> ContextCompat.getColor(context, R.color.colorPrimary)
            else -> ContextCompat.getColor(context, R.color.colorPositive)
        }
    }

    fun getDeviceId(): Long {
        return deviceId!!
    }

    @JsonProperty("device_id")
    fun setDeviceId(deviceId: Long?) {
        this.deviceId = deviceId
    }

    fun getUpdateMethodId(): Long {
        return updateMethodId!!
    }

    @JsonProperty("update_method_id")
    fun setUpdateMethodId(updateMethodId: Long?) {
        this.updateMethodId = updateMethodId
    }

    enum class ServerMessagePriority {
        LOW,
        MEDIUM,
        HIGH
    }

}


