package com.arjanvlek.oxygenupdater.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

import lombok.Getter
import lombok.Setter

@Getter
@Setter
class UpdateMethod {

    var id: Long = 0
        set(id) {
            field = this.id
        }

    @JsonProperty("english_name")
    var englishName: String? = null
        set(englishName) {
            field = this.englishName
        }

    @JsonProperty("dutch_name")
    var dutchName: String? = null
        set(dutchName) {
            field = this.dutchName
        }

    @JsonIgnore
    var isRecommended: Boolean = false
        private set

    @JsonProperty("recommended_for_rooted_device")
    var isRecommendedWithRoot: Boolean = false
        private set

    @JsonProperty("recommended_for_non_rooted_device")
    var isRecommendedWithoutRoot: Boolean = false
        private set

    @JsonProperty("supports_rooted_device")
    var isForRootedDevice: Boolean = false
        private set

    fun setRecommended(recommended: String?): UpdateMethod {
        this.isRecommended = recommended != null && recommended == "1"
        return this
    }

    fun setRecommendedWithRoot(recommendedWithRoot: String?) {
        this.isRecommendedWithRoot = recommendedWithRoot != null && recommendedWithRoot == "1"
    }

    fun setRecommendedWithoutRoot(recommendedWithoutRoot: String?) {
        this.isRecommendedWithoutRoot = recommendedWithoutRoot != null && recommendedWithoutRoot == "1"
    }

    fun setForRootedDevice(forRootedDevice: String?) {
        this.isForRootedDevice = forRootedDevice != null && forRootedDevice == "1"
    }
}
