package com.oxygenupdater.models

import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.oxygenupdater.BuildConfig

@Immutable // required because we have a List field (productNames)
data class Device(
    override val id: Long,
    override val name: String?,

    @JsonProperty("product_names")
    private val productNamesCsv: String?,

    val enabled: Boolean,
) : SelectableModel {

    @JsonIgnore
    val productNames = productNamesCsv?.trim()?.split(",")?.map { it.trim() } ?: listOf()

    companion object {
        private val IMAGE_URL_PREFIX = buildString(38) {
            append("https://")
            if (BuildConfig.DEBUG) append("test.")
            append("oxygenupdater.com/img/device/")
        }

        private const val IMAGE_URL_SUFFIX = "-min.png?v=1"

        fun constructImageUrl(deviceName: String) = IMAGE_URL_PREFIX +
                deviceName.split("(", limit = 2)[0].trim().replace(' ', '-').lowercase() +
                IMAGE_URL_SUFFIX
    }
}
