package com.oxygenupdater.ui

import androidx.compose.runtime.Immutable
import com.oxygenupdater.models.SelectableModel

@Immutable
data class SettingsListConfig<T : SelectableModel>(
    val list: List<T>,
    val initialIndex: Int,
    val selectedId: Long,
)
