package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Stable
data class InstallGuide(
    val id: Long = 0,
    val title: String = "",
    val subtitle: String = "",
    val body: String = "",
) : Parcelable {

    /** To preserve expand/collapse state in LazyColumn */
    @IgnoredOnParcel
    val expanded = mutableStateOf(false)
}
