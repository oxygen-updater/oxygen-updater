package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ServerMessage(
    val id: Long = 0,
    val englishMessage: String? = null,
    val dutchMessage: String? = null,
    val deviceId: Long? = null,
    val updateMethodId: Long? = null,
    val priority: ServerMessagePriority? = null,
) : Parcelable {

    @IgnoredOnParcel
    @JsonIgnore
    val text = if (AppLocale.get() == AppLocale.NL) dutchMessage ?: englishMessage else englishMessage

    enum class ServerMessagePriority {
        LOW,
        MEDIUM,
        HIGH
    }
}
