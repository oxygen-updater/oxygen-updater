package com.oxygenupdater.compose.ui

import androidx.compose.runtime.Immutable
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.UpdateMethod

/**
 * Compose doesn't treat [List]s as [stable][androidx.compose.runtime.StableMarker], so we need to wrap around it
 */
@Immutable
data class SettingsListWrapper(
    val enabledDevices: List<Device>,
    val methodsForDevice: List<UpdateMethod>,
)
