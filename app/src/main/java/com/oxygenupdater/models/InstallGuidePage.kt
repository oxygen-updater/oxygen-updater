package com.oxygenupdater.models

import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.oxygenupdater.models.AppLocale.NL

@JsonIgnoreProperties(ignoreUnknown = true)
@Immutable
data class InstallGuidePage(
    val id: Long,
    val deviceId: Long?,
    val updateMethodId: Long?,
    val pageNumber: Int,
    val fileExtension: String?,
    val imageUrl: String?,
    val useCustomImage: Boolean = false,

    @JsonProperty("title_en")
    val englishTitle: String?,

    @JsonProperty("title_nl")
    val dutchTitle: String?,

    @JsonProperty("text_en")
    val englishText: String?,

    @JsonProperty("text_nl")
    val dutchText: String?,
) {
    val isDefaultPage = deviceId == null || updateMethodId == null

    val title = if (AppLocale.get() == NL) dutchTitle else englishTitle
    val text = if (AppLocale.get() == NL) dutchText else englishText

    fun cloneWithDefaultTitleAndText(
        title: String,
        text: String,
    ) = copy(
        englishTitle = title,
        dutchTitle = title,
        englishText = text,
        dutchText = text
    )
}
