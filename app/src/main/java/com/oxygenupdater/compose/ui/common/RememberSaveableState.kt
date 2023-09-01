package com.oxygenupdater.compose.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

/** Warning: this can't be used if [value]s is an instance of a value class */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <T> rememberSaveableState(key: String, value: T) = rememberSaveable(key = key) {
    mutableStateOf(value)
}
