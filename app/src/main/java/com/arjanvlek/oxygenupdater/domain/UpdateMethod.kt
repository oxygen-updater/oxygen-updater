package com.arjanvlek.oxygenupdater.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class UpdateMethod(
    val id: Long,

    @JsonProperty("english_name")
    val englishName: String?,

    @JsonProperty("dutch_name")
    val dutchName: String?,

    @JsonIgnore
    var recommended: Boolean = false,

    var recommendedWithRoot: Boolean = false,
    var recommendedWithoutRoot: Boolean = false,
    var forRootedDevice: Boolean = false
) {

    fun setRecommended(recommended: String?): UpdateMethod {
        this.recommended = recommended != null && recommended == "1"
        return this
    }

    @JsonProperty("recommended_for_rooted_device")
    fun setRecommendedWithRoot(recommendedWithRoot: String?) {
        this.recommendedWithRoot = recommendedWithRoot != null && recommendedWithRoot == "1"
    }

    @JsonProperty("recommended_for_non_rooted_device")
    fun setRecommendedWithoutRoot(recommendedWithoutRoot: String?) {
        this.recommendedWithoutRoot = recommendedWithoutRoot != null && recommendedWithoutRoot == "1"
    }

    @JsonProperty("supports_rooted_device")
    fun setForRootedDevice(forRootedDevice: String?) {
        this.forRootedDevice = forRootedDevice != null && forRootedDevice == "1"
    }
}
