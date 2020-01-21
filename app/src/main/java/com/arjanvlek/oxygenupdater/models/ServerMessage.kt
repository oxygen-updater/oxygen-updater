package com.arjanvlek.oxygenupdater.models

import android.content.Context
import androidx.core.content.ContextCompat
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.i18n.AppLocale

data class ServerMessage(
    var id: Long = 0,
    var englishMessage: String? = null,
    var dutchMessage: String? = null,
    var deviceId: Long? = null,
    var updateMethodId: Long? = null,
    var priority: ServerMessagePriority? = null
) : Banner {

    override fun getBannerText(context: Context?): String? {
        return if (AppLocale.get() == AppLocale.NL) dutchMessage else englishMessage
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
