package com.arjanvlek.oxygenupdater.updateinformation

import android.content.Context
import androidx.core.content.ContextCompat
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.i18n.Locale
import com.fasterxml.jackson.annotation.JsonProperty

data class ServerMessage(
    var id: Long = 0,

    @set:JsonProperty("english_message")
    var englishMessage: String? = null,

    @set:JsonProperty("dutch_message")
    var dutchMessage: String? = null,

    @set:JsonProperty("device_id")
    var deviceId: Long? = null,

    @set:JsonProperty("update_method_id")
    var updateMethodId: Long? = null,

    var priority: ServerMessagePriority? = null
) : Banner {

    override fun getBannerText(context: Context?): String? {
        return if (Locale.locale == Locale.NL) dutchMessage else englishMessage
    }

    override fun getColor(context: Context?): Int {
        return when (priority) {
            ServerMessagePriority.LOW -> ContextCompat.getColor(context!!, R.color.colorPositive)
            ServerMessagePriority.MEDIUM -> ContextCompat.getColor(context!!, R.color.colorWarn)
            ServerMessagePriority.HIGH -> ContextCompat.getColor(context!!, R.color.colorPrimary)
            else -> ContextCompat.getColor(context!!, R.color.colorPositive)
        }
    }

    enum class ServerMessagePriority {
        LOW,
        MEDIUM,
        HIGH
    }
}
