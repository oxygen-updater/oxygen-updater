package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Stable
@JsonClass(generateAdapter = true)
data class InstallGuide(
    val id: Long,
    val title: String,
    val subtitle: String,
    val body: String,
) : Parcelable {

    /** To preserve expand/collapse state in LazyColumn */
    @IgnoredOnParcel
    val expanded = mutableStateOf(false)
}
