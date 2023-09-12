package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.oxygenupdater.internal.ForceBoolean
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Stable
@JsonClass(generateAdapter = true)
data class InAppFaq(
    val id: Long,

    val title: String?,
    val body: String?,
    @ForceBoolean val important: Boolean = false,

    /** Either `category` or `item` */
    val type: String,
) : Parcelable {

    /** To preserve expand/collapse state in LazyColumn */
    @IgnoredOnParcel
    val expanded = mutableStateOf(false)
}
