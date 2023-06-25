package com.oxygenupdater.models

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateData(
    var id: Long? = null,
    var versionNumber: String? = null,
    var otaVersionNumber: String? = null,
    var changelog: String? = null,
    var description: String? = null,
    var downloadUrl: String? = null,
    var downloadSize: Long = 0,
    var filename: String? = null,

    @JsonProperty("md5sum")
    var mD5Sum: String? = null,

    var information: String? = null,

    @JsonProperty("update_information_available")
    var updateInformationAvailable: Boolean = false,

    var systemIsUpToDate: Boolean = false,
) : FormattableUpdateData, Parcelable {

    @IgnoredOnParcel
    val isUpdateInformationAvailable = updateInformationAvailable || versionNumber != null

    // Formatting library: interface FormattableUpdateData
    @IgnoredOnParcel
    override val internalVersionNumber = versionNumber

    @IgnoredOnParcel
    override val updateDescription = description

    companion object {
        @JsonIgnore
        fun getBuildDate(otaVersionNumber: String?) = try {
            otaVersionNumber
                ?.substringAfterLast('_')
                ?.toLong() ?: 0
        } catch (e: NumberFormatException) {
            0
        }
    }
}
