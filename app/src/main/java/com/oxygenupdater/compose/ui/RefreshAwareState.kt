package com.oxygenupdater.compose.ui

import androidx.compose.runtime.Immutable

@Immutable
data class RefreshAwareState<out T>(
    val refreshing: Boolean,
    val data: T,
)
