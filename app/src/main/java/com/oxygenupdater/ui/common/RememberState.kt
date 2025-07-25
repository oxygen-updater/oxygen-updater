package com.oxygenupdater.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

/** Warning: this can't be used if [value] is an instance of a `value class` */
@Composable
inline fun <T> rememberSaveableState(value: T) = rememberSaveable { mutableStateOf(value) }

/** Warning: this can't be used if [value] is an instance of a `value class` */
@Composable
inline fun <T> rememberState(key1: Any?, value: T) = remember(key1) { mutableStateOf(value) }

/** Warning: this can't be used if [value] is an instance of a `value class` */
@Composable
inline fun <T> rememberState(value: T, policy: SnapshotMutationPolicy<T>) = remember { mutableStateOf(value, policy) }

/** Warning: this can't be used if [value] is an instance of a `value class` */
@Composable
inline fun <T> rememberState(value: T) = remember { mutableStateOf(value) }

@Composable
inline fun rememberState(value: Int) = remember { mutableIntStateOf(value) }

@Composable
inline fun rememberState(value: Float) = remember { mutableFloatStateOf(value) }
