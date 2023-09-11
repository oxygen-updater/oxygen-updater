@file:Suppress("NOTHING_TO_INLINE")

package com.oxygenupdater.ui.common

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
inline fun rememberCallback(
    noinline callback: @DisallowComposableCalls () -> Unit,
) = remember { callback }

@Composable
inline fun <T> rememberCallback(
    key1: Any?,
    noinline callback: @DisallowComposableCalls () -> T,
) = remember(key1) { callback }

@Composable
inline fun rememberCallback(
    key1: Any?,
    key2: Any?,
    noinline callback: @DisallowComposableCalls () -> Unit,
) = remember(key1, key2) { callback }

@Composable
inline fun <T, R> rememberTypedCallback(
    noinline callback: @DisallowComposableCalls (T) -> R,
) = remember { callback }

@Composable
inline fun <T, R> rememberTypedCallback(
    key1: Any?,
    noinline callback: @DisallowComposableCalls (T) -> R,
) = remember(key1) { callback }
