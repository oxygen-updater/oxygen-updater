package com.arjanvlek.oxygenupdater.models

import android.os.Parcel
import android.os.Parcelable
import android.util.SparseBooleanArray
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateData(
    var id: Long? = null,
    var versionNumber: String? = null,
    var otaVersionNumber: String? = null,
    var description: String? = null,
    var downloadUrl: String? = null,
    var downloadSize: Long = 0,
    var filename: String? = null,

    @JsonProperty("md5sum")
    var mD5Sum: String? = null,

    var information: String? = null,
    var updateInformationAvailable: Boolean = false,

    var systemIsUpToDate: Boolean = false
) : Parcelable, FormattableUpdateData {

    val downloadSizeInMegabytes = downloadSize / 1048576L

    val isUpdateInformationAvailable = updateInformationAvailable || versionNumber != null

    // Formatting library: interface FormattableUpdateData
    override val internalVersionNumber = versionNumber
    override val updateDescription = description

    @JsonProperty("update_information_available")
    fun setIsUpdateInformationAvailable(updateInformationAvailable: Boolean) {
        this.updateInformationAvailable = updateInformationAvailable
    }

    fun isSystemIsUpToDateCheck(settingsManager: SettingsManager?) = if (settingsManager != null
        && settingsManager.getPreference(SettingsManager.PROPERTY_ADVANCED_MODE, false)
    ) false
    else systemIsUpToDate

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id ?: -1L)
        dest.writeString(versionNumber)
        dest.writeString(otaVersionNumber)
        dest.writeString(description)
        dest.writeString(downloadUrl)
        dest.writeLong(downloadSize)
        dest.writeString(filename)
        dest.writeString(mD5Sum)
        dest.writeString(information)

        SparseBooleanArray().apply {
            put(0, updateInformationAvailable)
            put(1, systemIsUpToDate)

            dest.writeSparseBooleanArray(this)
        }
    }

    companion object {
        val CREATOR = object : Parcelable.Creator<UpdateData> {
            override fun createFromParcel(parcel: Parcel): UpdateData? {
                val data = UpdateData(
                    id = parcel.readLong(),
                    versionNumber = parcel.readString(),
                    otaVersionNumber = parcel.readString(),
                    description = parcel.readString(),
                    downloadUrl = parcel.readString(),
                    downloadSize = parcel.readLong(),
                    filename = parcel.readString(),
                    mD5Sum = parcel.readString(),
                    information = parcel.readString()
                )

                parcel.readSparseBooleanArray()?.let {
                    data.setIsUpdateInformationAvailable(it[0])
                    data.systemIsUpToDate = it[1]
                }

                return data
            }

            override fun newArray(size: Int) = arrayOfNulls<UpdateData?>(size)
        }
    }
}
