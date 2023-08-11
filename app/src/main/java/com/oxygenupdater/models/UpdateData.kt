package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.work.Data
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonIgnoreProperties(ignoreUnknown = true)
@Immutable
data class UpdateData(
    val id: Long? = null,
    val versionNumber: String? = null,
    val otaVersionNumber: String? = null,
    val changelog: String? = null,
    val description: String? = null,
    val downloadUrl: String? = null,
    val downloadSize: Long = 0,
    val filename: String? = null,

    @JsonProperty("md5sum")
    val mD5Sum: String? = null,

    val information: String? = null,

    @JsonProperty("update_information_available")
    val updateInformationAvailable: Boolean = false,

    val systemIsUpToDate: Boolean = false,
) : Parcelable {

    @IgnoredOnParcel
    val isUpdateInformationAvailable = updateInformationAvailable || versionNumber != null

    fun toWorkData() = Data.Builder().apply {
        putLong("id", id ?: -1L)
        putString("versionNumber", versionNumber)
        putString("otaVersionNumber", otaVersionNumber)
        putString("description", description)
        putString("downloadUrl", downloadUrl)
        putLong("downloadSize", downloadSize)
        putString("filename", filename)
        putString("mD5Sum", mD5Sum)
        putString("information", information)
        putBoolean("updateInformationAvailable", updateInformationAvailable)
        putBoolean("systemIsUpToDate", systemIsUpToDate)
    }.build()

    companion object {
        @JsonIgnore
        fun getBuildDate(otaVersionNumber: String?) = otaVersionNumber?.substringAfterLast('_')?.toLongOrNull() ?: 0

        @JsonIgnore
        fun createFromWorkData(inputData: Data?) = if (inputData != null) UpdateData(
            id = inputData.getLong("id", -1L),
            versionNumber = inputData.getString("versionNumber"),
            otaVersionNumber = inputData.getString("otaVersionNumber"),
            description = inputData.getString("description"),
            downloadUrl = inputData.getString("downloadUrl"),
            downloadSize = inputData.getLong("downloadSize", -1L),
            filename = inputData.getString("filename"),
            mD5Sum = inputData.getString("mD5Sum"),
            information = inputData.getString("information"),
            updateInformationAvailable = inputData.getBoolean("updateInformationAvailable", false),
            systemIsUpToDate = inputData.getBoolean("systemIsUpToDate", false)
        ) else null
    }
}
