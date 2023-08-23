package com.oxygenupdater.compose.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember

/**
 * `Scaffold`'s `innerPadding` with `TopAppBar.scrollBehaviour` causes a recompose
 * for as long as `TopAppBar` is collapsing, meaning Screen composables are not skipped
 * because lambdas are recreated each time. We "fix" this by remembering lambdas.
 */
@Composable
fun rememberCallback(
    callback: @DisallowComposableCalls () -> Unit,
) = remember { callback }

@Composable
fun <T, R> rememberTypedCallback(
    callback: @DisallowComposableCalls (T) -> R,
) = remember { callback }

@Composable
fun <T, R> rememberTypedCallback(
    vararg keys: Any?,
    callback: @DisallowComposableCalls (T) -> R,
) = remember(keys) { callback }
