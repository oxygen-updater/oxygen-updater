package com.oxygenupdater.models

import androidx.annotation.VisibleForTesting
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
        @VisibleForTesting
        val ImageUrlPrefix = buildString(37) {
            append(BuildConfig.SERVER_DOMAIN)
            append("img/device/")
        }

        @VisibleForTesting
        const val ImageUrlSuffix = "-min.png?v=1"

        fun constructImageUrl(deviceName: String) = ImageUrlPrefix +
                deviceName.split("(", limit = 2)[0].trim().replace(' ', '-').lowercase() +
                ImageUrlSuffix
    }
}
