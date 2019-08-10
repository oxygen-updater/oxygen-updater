package com.arjanvlek.oxygenupdater.installation.manual

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class InstallGuidePage {

    var id: Long? = null
    @set:JsonProperty(value = "device_id")
    var deviceId: Long? = null
    @set:JsonProperty(value = "update_method_id")
    var updateMethodId: Long? = null
    @set:JsonProperty(value = "page_number")
    var pageNumber: Int? = null
    @set:JsonProperty(value = "file_extension")
    var fileExtension: String? = null
    @set:JsonProperty(value = "image_url")
    var imageUrl: String? = null
    var useCustomImage: Boolean? = null
        private set
    @set:JsonProperty(value = "title_en")
    var englishTitle: String? = null
    @set:JsonProperty(value = "title_nl")
    var dutchTitle: String? = null
    @set:JsonProperty(value = "text_en")
    var englishText: String? = null
    @set:JsonProperty(value = "text_nl")
    var dutchText: String? = null

    @JsonProperty(value = "use_custom_image")
    fun setUseCustomImage(useCustomImage: String?) {
        this.useCustomImage = useCustomImage != null && useCustomImage == "1"
    }
}
