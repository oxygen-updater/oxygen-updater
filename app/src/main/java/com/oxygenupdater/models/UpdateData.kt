package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.work.Data
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.compose.ui.onboarding.NOT_SET_L
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "update_data")
@JsonIgnoreProperties(ignoreUnknown = true)
@Immutable
data class UpdateData(
    @PrimaryKey
    val id: Long?,

    @ColumnInfo("version_number")
    val versionNumber: String? = null,

    @ColumnInfo("ota_version_number")
    val otaVersionNumber: String? = null,

    val changelog: String? = null,
    val description: String? = null,

    @ColumnInfo("download_url")
    val downloadUrl: String? = null,

    @ColumnInfo("download_size")
    val downloadSize: Long = 0,

    val filename: String? = null,
    val md5sum: String? = null,
    val information: String? = null,

    @ColumnInfo("update_information_available")
    @JsonProperty("update_information_available", defaultValue = "0")
    val updateInformationAvailable: Boolean = false,

    @ColumnInfo("system_is_up_to_date", defaultValue = "0")
    val systemIsUpToDate: Boolean = false,
) : Parcelable {

    @IgnoredOnParcel
    @Ignore
    @JvmField
    val isUpdateInformationAvailable = updateInformationAvailable || versionNumber != null

    @IgnoredOnParcel
    @Ignore
    @JvmField
    val shouldFetchMostRecent = information != null && information == OxygenUpdater.UNABLE_TO_FIND_A_MORE_RECENT_BUILD && isUpdateInformationAvailable && systemIsUpToDate

    fun toWorkData() = Data.Builder().apply {
        putLong("id", id ?: NOT_SET_L)
        putString("versionNumber", versionNumber)
        putString("otaVersionNumber", otaVersionNumber)
        putString("description", description)
        putString("downloadUrl", downloadUrl)
        putLong("downloadSize", downloadSize)
        putString("filename", filename)
        putString("md5sum", md5sum)
        putString("information", information)
        putBoolean("updateInformationAvailable", updateInformationAvailable)
        putBoolean("systemIsUpToDate", systemIsUpToDate)
    }.build()

    companion object {
        @JsonIgnore
        fun getBuildDate(otaVersionNumber: String?) = otaVersionNumber?.substringAfterLast('_')?.toLongOrNull() ?: 0

        @JsonIgnore
        fun createFromWorkData(inputData: Data?) = if (inputData != null) UpdateData(
            id = inputData.getLong("id", NOT_SET_L),
            versionNumber = inputData.getString("versionNumber"),
            otaVersionNumber = inputData.getString("otaVersionNumber"),
            description = inputData.getString("description"),
            downloadUrl = inputData.getString("downloadUrl"),
            downloadSize = inputData.getLong("downloadSize", NOT_SET_L),
            filename = inputData.getString("filename"),
            md5sum = inputData.getString("md5sum"),
            information = inputData.getString("information"),
            updateInformationAvailable = inputData.getBoolean("updateInformationAvailable", false),
            systemIsUpToDate = inputData.getBoolean("systemIsUpToDate", false)
        ) else null
    }
}
