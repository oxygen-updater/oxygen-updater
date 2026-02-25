package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.oxygenupdater.internal.ForceBoolean
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Stable
data class InAppFaq(
    val id: Long = 0,

    val title: String? = null,
    val body: String? = null,
    val important: ForceBoolean = false,

    /** Either `category` or `item` */
    val type: String = "category", // categories are rendered as Text, reasonable default
) : Parcelable {

    /** To preserve expand/collapse state in LazyColumn */
    @IgnoredOnParcel
    val expanded = mutableStateOf(false)
}
