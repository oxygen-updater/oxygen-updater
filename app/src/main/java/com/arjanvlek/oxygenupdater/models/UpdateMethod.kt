package com.arjanvlek.oxygenupdater.models

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdateMethod(
    override val id: Long,
    val englishName: String?,
    val dutchName: String?,
    var recommended: Boolean = false,
    val recommendedForRootedDevice: Boolean = false,
    val recommendedForNonRootedDevice: Boolean = false,
    val supportsRootedDevice: Boolean = false
) : SelectableModel {

    override val name = englishName

    @JsonProperty("recommended")
    fun setRecommended(recommended: String?): UpdateMethod {
        this.recommended = recommended != null && recommended == "1"
        return this
    }
}
