package com.oxygenupdater.ui

import androidx.compose.runtime.Immutable

@Immutable
data class RefreshAwareState<T>(
    val refreshing: Boolean,
    val data: T,
)
