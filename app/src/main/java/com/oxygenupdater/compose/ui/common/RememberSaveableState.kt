package com.oxygenupdater.compose.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.structuralEqualityPolicy

/** Warning: this can't be used if [value]s is an instance of a value class */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <T> rememberSaveableState(key: String, value: T, referential: Boolean = false) = rememberSaveable(key = key) {
    mutableStateOf(value, if (referential) referentialEqualityPolicy() else structuralEqualityPolicy())
}

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun rememberSaveableState(key: String, value: Int) = rememberSaveable(key = key) {
    mutableIntStateOf(value)
}
