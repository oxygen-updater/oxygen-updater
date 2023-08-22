package com.oxygenupdater.models

import androidx.compose.runtime.Immutable

@Immutable
data class UpdateMethod(
    override val id: Long,
    override val name: String?,
    val recommendedForRootedDevice: Boolean = false,
    val recommendedForNonRootedDevice: Boolean = false,
    val supportsRootedDevice: Boolean = false,
) : SelectableModel
