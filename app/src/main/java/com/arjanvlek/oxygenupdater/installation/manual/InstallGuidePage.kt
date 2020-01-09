package com.arjanvlek.oxygenupdater.installation.manual

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallGuidePage(
    val id: Long,

    @JsonProperty("device_id")
    var deviceId: Long?,

    @JsonProperty("update_method_id")
    var updateMethodId: Long?,

    @JsonProperty("page_number")
    var pageNumber: Int,

    @JsonProperty("file_extension")
    var fileExtension: String,

    @JsonProperty("image_url")
    var imageUrl: String,

    var useCustomImage: Boolean,

    @JsonProperty("title_en")
    var englishTitle: String,

    @JsonProperty("title_nl")
    var dutchTitle: String,

    @JsonProperty("text_en")
    var englishText: String,

    @JsonProperty("text_nl")
    var dutchText: String
) {

    @JsonProperty("use_custom_image")
    fun setUseCustomImage(useCustomImage: String?) {
        this.useCustomImage = useCustomImage != null && useCustomImage == "1"
    }

}
