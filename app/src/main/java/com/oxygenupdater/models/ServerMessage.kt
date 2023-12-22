package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
@JsonClass(generateAdapter = true)
data class ServerMessage(
    val id: Long = 0,
    val text: String? = null,
    val priority: ServerMessagePriority? = null,
) : Parcelable {

    @JsonClass(generateAdapter = false) // https://github.com/square/moshi#enums
    enum class ServerMessagePriority {
        LOW,
        MEDIUM,
        HIGH
    }
}
