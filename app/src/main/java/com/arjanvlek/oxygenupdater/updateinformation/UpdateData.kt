package com.arjanvlek.oxygenupdater.updateinformation

import android.os.Parcel
import android.os.Parcelable
import android.util.SparseBooleanArray
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.versionformatter.FormattableUpdateData
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateData(
    var id: Long? = null,

    @set:JsonProperty("version_number")
    var versionNumber: String? = null,

    @set:JsonProperty("ota_version_number")
    var otaVersionNumber: String? = null,

    var description: String? = null,

    @set:JsonProperty("download_url")
    var downloadUrl: String? = null,

    @set:JsonProperty("download_size")
    var downloadSize: Long = 0,

    var filename: String? = null,

    @set:JsonProperty("md5sum")
    var mD5Sum: String? = null,

    var information: String? = null,

    private var updateInformationAvailable: Boolean = false,

    @set:JsonProperty("system_is_up_to_date")
    var isSystemIsUpToDate: Boolean = false
) : Parcelable, FormattableUpdateData {

    val downloadSizeInMegabytes: Long
        get() = downloadSize / 1048576L

    // Formatting library: interface FormattableUpdateData
    override val internalVersionNumber: String?
        get() = versionNumber
    override val updateDescription: String?
        get() = description

    constructor(parcel: Parcel) : this() {
        id = parcel.readValue(Long::class.java.classLoader) as? Long
        otaVersionNumber = parcel.readString()
        downloadUrl = parcel.readString()
        downloadSize = parcel.readLong()
        filename = parcel.readString()
        mD5Sum = parcel.readString()
        information = parcel.readString()
        updateInformationAvailable = parcel.readByte() != 0.toByte()
        isSystemIsUpToDate = parcel.readByte() != 0.toByte()
    }

    fun isUpdateInformationAvailable(): Boolean {
        return updateInformationAvailable || versionNumber != null
    }

    @JsonProperty("update_information_available")
    fun setUpdateInformationAvailable(updateInformationAvailable: Boolean) {
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

        SparseBooleanArray().apply {
            put(0, updateInformationAvailable)
            put(1, isSystemIsUpToDate)

            dest.writeSparseBooleanArray(this)
        }
    }

    companion object CREATOR : Parcelable.Creator<UpdateData> {
        override fun createFromParcel(parcel: Parcel): UpdateData? {
            val data = UpdateData()
            data.id = parcel.readLong()
            data.versionNumber = parcel.readString()
            data.otaVersionNumber = parcel.readString()
            data.description = parcel.readString()
            data.downloadUrl = parcel.readString()
            data.downloadSize = parcel.readLong()
            data.filename = parcel.readString()
            data.mD5Sum = parcel.readString()
            data.information = parcel.readString()

            parcel.readSparseBooleanArray()?.let {
                data.setUpdateInformationAvailable(it[0])
                data.isSystemIsUpToDate = it[1]
            }

            return data
        }

        override fun newArray(size: Int): Array<UpdateData?> {
            return arrayOfNulls(size)
        }
    }
}
