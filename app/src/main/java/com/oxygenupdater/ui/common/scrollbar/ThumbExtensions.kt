package com.oxygenupdater.ui.common.scrollbar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

/**
 * Remembers a function to react to [Scrollbar] thumb position displacements for a [ScrollState].
 */
@Composable
fun ScrollState.rememberDraggableScroller() = rememberDraggableScroller(
    limit = maxValue,
    scroll = ::scrollTo,
)

/**
 * Remembers a function to react to [Scrollbar] thumb position displacements for a [LazyListState].
 */
@Composable
fun LazyListState.rememberDraggableScroller() = rememberDraggableScroller(
    limit = layoutInfo.totalItemsCount,
    scroll = ::scrollToItem,
)

/**
 * Remembers a function to react to [Scrollbar] thumb position displacements for a [LazyGridState].
 */
@Composable
fun LazyGridState.rememberDraggableScroller() = rememberDraggableScroller(
    limit = layoutInfo.totalItemsCount,
    scroll = ::scrollToItem,
)

/**
 * Remembers a function to react to [Scrollbar] thumb position displacements for a [LazyStaggeredGridState].
 */
@Composable
fun LazyStaggeredGridState.rememberDraggableScroller() = rememberDraggableScroller(
    limit = layoutInfo.totalItemsCount,
    scroll = ::scrollToItem,
)

/**
 * Generic function to react to [Scrollbar] thumb displacements in a lazy layout, or a generic
 * scroll container.
 *
 * @param limit for list-backed states, the total amount of items available to scroll in the
 *        layout; otherwise, the max pixel limit for the scrollable container.
 * @param scroll a function to be invoked when an index (or px) has been identified to scroll to.
 */
@Composable
private inline fun rememberDraggableScroller(
    limit: Int,
    crossinline scroll: suspend (value: Int) -> Unit,
): (Float) -> Unit {
    var percentage by remember { mutableFloatStateOf(Float.NaN) }
    val limit by rememberUpdatedState(limit)

    LaunchedEffect(percentage) {
        if (percentage.isNaN()) return@LaunchedEffect

        val value = (limit * percentage).roundToInt()
        scroll(value)
    }

    return remember {
        { newPercentage -> percentage = newPercentage }
    }
}
