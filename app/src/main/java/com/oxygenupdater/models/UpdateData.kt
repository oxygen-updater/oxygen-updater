package com.oxygenupdater.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.oxygenupdater.OxygenUpdater
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Parcelize
@Immutable
@Entity(tableName = "update_data")
data class UpdateData(
    @PrimaryKey
    val id: Long? = 0,

    @ColumnInfo("version_number")
    @JsonNames("version_number")
    val versionNumber: String? = null,

    @ColumnInfo("ota_version_number")
    @JsonNames("ota_version_number")
    val otaVersionNumber: String? = null,

    val changelog: String? = null,
    val description: String? = null,

    @ColumnInfo("download_url")
    @JsonNames("download_url")
    val downloadUrl: String? = null,

    @ColumnInfo("download_size")
    @JsonNames("download_size")
    val downloadSize: Long = 0,

    val filename: String? = null,
    val md5sum: String? = null,
    val information: String? = null,

    @ColumnInfo("update_information_available")
    @JsonNames("update_information_available")
    val updateInformationAvailable: Boolean = false,

    @ColumnInfo("system_is_up_to_date", defaultValue = "0")
    @JsonNames("system_is_up_to_date")
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
