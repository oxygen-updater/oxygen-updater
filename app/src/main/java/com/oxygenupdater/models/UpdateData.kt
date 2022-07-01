package com.oxygenupdater.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

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
    var updateInformationAvailable: Boolean = false,

    var systemIsUpToDate: Boolean = false
) : FormattableUpdateData {

    val isUpdateInformationAvailable = updateInformationAvailable || versionNumber != null

    // Formatting library: interface FormattableUpdateData
    override val internalVersionNumber = versionNumber
    override val updateDescription = description

    @JsonProperty("update_information_available")
    fun setIsUpdateInformationAvailable(updateInformationAvailable: Boolean) {
        this.updateInformationAvailable = updateInformationAvailable
    }
}
