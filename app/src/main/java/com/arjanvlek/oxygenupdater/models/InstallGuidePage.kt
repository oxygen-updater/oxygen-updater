package com.arjanvlek.oxygenupdater.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallGuidePage(
    val id: Long,
    var deviceId: Long?,
    var updateMethodId: Long?,
    var pageNumber: Int,
    var fileExtension: String?,
    var imageUrl: String?,
    var useCustomImage: Boolean?,

    @JsonProperty("title_en")
    var englishTitle: String,

    @JsonProperty("title_nl")
    var dutchTitle: String,

    @JsonProperty("text_en")
    var englishText: String,

    @JsonProperty("text_nl")
    var dutchText: String
)
