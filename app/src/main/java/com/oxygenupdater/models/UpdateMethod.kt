package com.oxygenupdater.models

import androidx.compose.runtime.Immutable

@Immutable
data class UpdateMethod(
    override val id: Long,
    val englishName: String?,
    val dutchName: String?,
    val recommendedForRootedDevice: Boolean = false,
    val recommendedForNonRootedDevice: Boolean = false,
    val supportsRootedDevice: Boolean = false,
) : SelectableModel {
    override val name = if (AppLocale.get() == AppLocale.NL) dutchName else englishName
}
