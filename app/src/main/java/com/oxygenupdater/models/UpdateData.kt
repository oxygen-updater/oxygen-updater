package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.oxygenupdater.OxygenUpdater
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
@Entity(tableName = "update_data")
@JsonClass(generateAdapter = true)
data class UpdateData(
    @PrimaryKey
    val id: Long?,

    @ColumnInfo("version_number")
    @Json(name = "version_number")
    val versionNumber: String? = null,

    @ColumnInfo("ota_version_number")
    @Json(name = "ota_version_number")
    val otaVersionNumber: String? = null,

    val changelog: String? = null,
    val description: String? = null,

    @ColumnInfo("download_url")
    @Json(name = "download_url")
    val downloadUrl: String? = null,

    @ColumnInfo("download_size")
    @Json(name = "download_size")
    val downloadSize: Long = 0,

    val filename: String? = null,
    val md5sum: String? = null,
    val information: String? = null,

    @ColumnInfo("update_information_available")
    @Json(name = "update_information_available")
    val updateInformationAvailable: Boolean = false,

    @ColumnInfo("system_is_up_to_date", defaultValue = "0")
    @Json(name = "system_is_up_to_date")
    val systemIsUpToDate: Boolean = false,
) : Parcelable {

    @IgnoredOnParcel
    @Ignore
    @JvmField
    val isUpdateInformationAvailable = updateInformationAvailable || versionNumber != null

    @IgnoredOnParcel
    @Ignore
    @JvmField
    val shouldFetchMostRecent = information != null && information == OxygenUpdater.UnableToFindAMoreRecentBuild && isUpdateInformationAvailable && systemIsUpToDate

    companion object {
        fun getBuildDate(otaVersionNumber: String?) = otaVersionNumber?.substringAfterLast('_')?.toLongOrNull() ?: 0
    }
}
