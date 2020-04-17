package com.arjanvlek.oxygenupdater.models

import com.arjanvlek.oxygenupdater.models.AppLocale.NL
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
    var useCustomImage: Boolean = false,

    @JsonProperty("title_en")
    var englishTitle: String?,

    @JsonProperty("title_nl")
    var dutchTitle: String?,

    @JsonProperty("text_en")
    var englishText: String?,

    @JsonProperty("text_nl")
    var dutchText: String?
) {
    val isDefaultPage = deviceId == null || updateMethodId == null

    val title = if (AppLocale.get() == NL) dutchTitle else englishTitle
    val text = if (AppLocale.get() == NL) dutchText else englishText

    fun cloneWithDefaultTitleAndText(
        title: String,
        text: String
    ) = copy(
        englishTitle = title,
        dutchTitle = title,
        englishText = text,
        dutchText = text
    )
}
