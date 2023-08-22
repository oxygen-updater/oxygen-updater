package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ServerMessage(
    val id: Long = 0,
    val text: String? = null,
    val deviceId: Long? = null,
    val updateMethodId: Long? = null,
    val priority: ServerMessagePriority? = null,
) : Parcelable {

    enum class ServerMessagePriority {
        LOW,
        MEDIUM,
        HIGH
    }
}
