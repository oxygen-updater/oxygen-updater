package com.oxygenupdater.models

import androidx.compose.runtime.Immutable
import com.oxygenupdater.internal.ForceBoolean
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class UpdateMethod(
    override val id: Long,
    override val name: String?,

    @Json(name = "recommended_for_rooted_device")
    @ForceBoolean val recommendedForRootedDevice: Boolean = false,

    @Json(name = "recommended_for_non_rooted_device")
    @ForceBoolean val recommendedForNonRootedDevice: Boolean = false,

    @Json(name = "supports_rooted_device")
    @ForceBoolean val supportsRootedDevice: Boolean = false,
) : SelectableModel {
    override val subtitle = null
}
