package com.oxygenupdater.models

import androidx.compose.runtime.Immutable
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.internal.CsvList
import com.oxygenupdater.internal.ForceBoolean
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable // required because we have a List field (productNames)
@JsonClass(generateAdapter = true)
data class Device(
    override val id: Long,
    override val name: String?,

    @Json(name = "product_names")
    @CsvList val productNames: List<String>,

    @ForceBoolean val enabled: Boolean,
) : SelectableModel {

    companion object {
        private val IMAGE_URL_PREFIX = buildString(37) {
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
