package com.arjanvlek.oxygenupdater.updateinformation

import android.os.Parcel
import android.os.Parcelable
import android.util.SparseBooleanArray

import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.versionformatter.FormattableUpdateData
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdateData : Parcelable, FormattableUpdateData {
    var id: Long? = null
    @set:JsonProperty("version_number")
    var versionNumber: String? = null
    @set:JsonProperty("ota_version_number")
    var otaVersionNumber: String? = null
    var description: String? = null
    @set:JsonProperty("download_url")
    var downloadUrl: String? = null
    @set:JsonProperty("download_size")
    var downloadSize: Long = 0
    var filename: String? = null
    @set:JsonProperty("md5sum")
    var mD5Sum: String? = null
    var information: String? = null
    private var updateInformationAvailable: Boolean = false
    @set:JsonProperty("system_is_up_to_date")
    var isSystemIsUpToDate: Boolean = false

    val downloadSizeInMegabytes: Long
        get() = downloadSize / 1048576L

    var isUpdateInformationAvailable: Boolean
        get() = updateInformationAvailable || versionNumber != null
        @JsonProperty("update_information_available")
        set(updateInformationAvailable) {
            this.updateInformationAvailable = updateInformationAvailable
        }

    fun isSystemIsUpToDateCheck(settingsManager: SettingsManager?): Boolean {

        return if (settingsManager != null && settingsManager.getPreference(SettingsManager.PROPERTY_ADVANCED_MODE, false)) {
            false
        } else isSystemIsUpToDate

    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        if (id != null) {
            dest.writeLong(id!!)
        } else {
            dest.writeLong(-1L)
        }

        dest.writeString(versionNumber)
        dest.writeString(otaVersionNumber)
        dest.writeString(description)
        dest.writeString(downloadUrl)
        dest.writeLong(downloadSize)
        dest.writeString(filename)
        dest.writeString(mD5Sum)
        dest.writeString(information)

        val booleanValues = SparseBooleanArray()
        booleanValues.put(0, updateInformationAvailable)
        booleanValues.put(1, isSystemIsUpToDate)

        dest.writeSparseBooleanArray(booleanValues)
    }

    // Formatting library: interface FormattableUpdateData

    override val internalVersionNumber: String
        get() = versionNumber!!

    override val updateDescription: String
        get() = description!!

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<UpdateData> = object : Parcelable.Creator<UpdateData> {

            override fun createFromParcel(`in`: Parcel): UpdateData {
                val data = UpdateData()

                data.id = `in`.readLong()
                data.versionNumber = `in`.readString()
                data.otaVersionNumber = `in`.readString()
                data.description = `in`.readString()
                data.downloadUrl = `in`.readString()
                data.downloadSize = `in`.readLong()
                data.filename = `in`.readString()
                data.mD5Sum = `in`.readString()
                data.information = `in`.readString()

                val booleanValues = `in`.readSparseBooleanArray()
                data.isUpdateInformationAvailable = booleanValues.get(0)
                data.isSystemIsUpToDate = booleanValues.get(1)
                return data
            }

            override fun newArray(size: Int): Array<UpdateData?> {
                return arrayOfNulls(size)
            }
        }
    }
}
