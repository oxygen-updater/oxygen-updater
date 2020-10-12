package com.oxygenupdater.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseBooleanArray
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

    /**
     * This value is used in [android.text.format.Formatter.formatFileSize],
     * which formats differently on different API levels.
     * Pre-Oreo (API Level 26), the IEC format is used, while SI units are used on Oreo and above.
     *
     * Our admin portal calculates everything in IEC units (mebibyte instead of megabyte, for example).
     * Using this value guarantees consistency with admin portal values.
     *
     * Note: 1 MiB =  1048576 bytes, while 1 MB = 1000000 bytes
     */
    val downloadSizeForFormatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        (downloadSize / 1.048576).toLong()
    } else {
        downloadSize
    }

    val isUpdateInformationAvailable = updateInformationAvailable || versionNumber != null

    // Formatting library: interface FormattableUpdateData
    override val internalVersionNumber = versionNumber
    override val updateDescription = description

    @JsonProperty("update_information_available")
    fun setIsUpdateInformationAvailable(updateInformationAvailable: Boolean) {
        this.updateInformationAvailable = updateInformationAvailable
    }

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
