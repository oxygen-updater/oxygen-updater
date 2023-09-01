package com.oxygenupdater.compose.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember

/**
 * Lambdas that have references to unstable objects are not memoized by Compose,
 * even if those objects are `Context`, `ViewModel`, and other things that remain
 * unchanged throughout the composable's lifecycle.
 *
 * We need to explicitly remember them to avoid unnecessarily recomposing children.
 */
@Composable
fun rememberCallback(
    callback: @DisallowComposableCalls () -> Unit,
) = remember { callback }

@Composable
fun rememberCallback(
    key1: Any?,
    callback: @DisallowComposableCalls () -> Unit,
) = remember(key1) { callback }

@Composable
fun rememberCallback(
    key1: Any?,
    key2: Any?,
    callback: @DisallowComposableCalls () -> Unit,
) = remember(key1, key2) { callback }

@Composable
fun <T, R> rememberTypedCallback(
    callback: @DisallowComposableCalls (T) -> R,
) = remember { callback }

@Composable
fun <T, R> rememberTypedCallback(
    key1: Any?,
    callback: @DisallowComposableCalls (T) -> R,
) = remember(key1) { callback }
