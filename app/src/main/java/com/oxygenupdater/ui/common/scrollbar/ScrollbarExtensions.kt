package com.oxygenupdater.ui.common.scrollbar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastCoerceAtLeast
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.min

/**
 * Calculates a [ScrollbarState] driven by the changes in a [ScrollState].
 */
@Composable
fun ScrollState.scrollbarState() = remember { ScrollbarState() }.also { state ->
    LaunchedEffect(this) {
        snapshotFlow {
            if (maxValue <= 0 || maxValue == Int.MAX_VALUE) return@snapshotFlow null
            if (viewportSize <= 0 || viewportSize == maxValue) return@snapshotFlow null

            val maxValueFloat = maxValue.toFloat().fastCoerceAtLeast(1f)
            scrollbarStateValue(
                thumbSizePercent = min(viewportSize / maxValueFloat, 1f),
                thumbMovedPercent = min(value / maxValueFloat, 1f),
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state.onScroll(it) }
    }
}

/**
 * Calculates a [ScrollbarState] driven by the changes in a [LazyListState].
 */
@Composable
fun LazyListState.scrollbarState() = remember { ScrollbarState() }.also { state ->
    val numItems = layoutInfo.totalItemsCount
    val itemIndex = LazyListItemInfo::index

    LaunchedEffect(this, numItems) {
        snapshotFlow {
            if (numItems == 0) return@snapshotFlow null

            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val firstIndex = min(
                interpolateFirstItemIndex(
                    visibleItems = visibleItemsInfo,
                    itemSize = { it.size },
                    offset = { it.offset },
                    nextItemOnMainAxis = { first -> visibleItemsInfo.find { it != first } },
                    itemIndex = itemIndex,
                ),
                numItems.toFloat(),
            )
            if (firstIndex.isNaN()) return@snapshotFlow null

            val itemsVisible = visibleItemsInfo.floatSumOf { itemInfo ->
                itemVisibilityPercentage(
                    itemSize = itemInfo.size,
                    itemStartOffset = itemInfo.offset,
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                    viewportEndOffset = layoutInfo.viewportEndOffset,
                )
            }

            val numItemsMin1 = numItems.fastCoerceAtLeast(1)
            val thumbMovedPercent = min(firstIndex / numItemsMin1, 1f)
            scrollbarStateValue(
                thumbSizePercent = min(itemsVisible / numItemsMin1, 1f),
                thumbMovedPercent = when {
                    layoutInfo.reverseLayout -> 1f - thumbMovedPercent
                    else -> thumbMovedPercent
                },
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state.onScroll(it) }
    }
}

/**
 * Calculates a [ScrollbarState] driven by the changes in a [LazyGridState].
 */
@Composable
fun LazyGridState.scrollbarState() = remember { ScrollbarState() }.also { state ->
    val numItems = layoutInfo.totalItemsCount
    val itemIndex = LazyGridItemInfo::index

    LaunchedEffect(this, numItems) {
        snapshotFlow {
            if (numItems == 0) return@snapshotFlow null

            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val orientation = layoutInfo.orientation
            val firstIndex = min(
                interpolateFirstItemIndex(
                    visibleItems = visibleItemsInfo,
                    itemSize = { orientation.valueOf(it.size) },
                    offset = { orientation.valueOf(it.offset) },
                    nextItemOnMainAxis = { first ->
                        when (orientation) {
                            Orientation.Vertical -> visibleItemsInfo.find {
                                it != first && it.row != first.row
                            }

                            Orientation.Horizontal -> visibleItemsInfo.find {
                                it != first && it.column != first.column
                            }
                        }
                    },
                    itemIndex = itemIndex,
                ),
                numItems.toFloat(),
            )
            if (firstIndex.isNaN()) return@snapshotFlow null

            val itemsVisible = visibleItemsInfo.floatSumOf { itemInfo ->
                itemVisibilityPercentage(
                    itemSize = orientation.valueOf(itemInfo.size),
                    itemStartOffset = orientation.valueOf(itemInfo.offset),
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                    viewportEndOffset = layoutInfo.viewportEndOffset,
                )
            }

            val numItemsMin1 = numItems.fastCoerceAtLeast(1)
            val thumbMovedPercent = min(firstIndex / numItemsMin1, 1f)
            scrollbarStateValue(
                thumbSizePercent = min(itemsVisible / numItemsMin1, 1f),
                thumbMovedPercent = when {
                    layoutInfo.reverseLayout -> 1f - thumbMovedPercent
                    else -> thumbMovedPercent
                },
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state.onScroll(it) }
    }
}

/**
 * Remembers a [ScrollbarState] driven by the changes in a [LazyStaggeredGridState].
 *
 * @param itemsAvailable the total amount of items available to scroll in the staggered grid.
 */
@Composable
fun LazyStaggeredGridState.scrollbarState(itemsAvailable: Int) = remember { ScrollbarState() }.also { state ->
    val numItems = layoutInfo.totalItemsCount
    val itemIndex = LazyStaggeredGridItemInfo::index

    LaunchedEffect(this, numItems) {
        snapshotFlow {
            if (numItems == 0) return@snapshotFlow null

            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val orientation = layoutInfo.orientation
            val firstIndex = min(
                interpolateFirstItemIndex(
                    visibleItems = visibleItemsInfo,
                    itemSize = { orientation.valueOf(it.size) },
                    offset = { orientation.valueOf(it.offset) },
                    nextItemOnMainAxis = { first ->
                        visibleItemsInfo.find { it != first && it.lane == first.lane }
                    },
                    itemIndex = itemIndex,
                ),
                numItems.toFloat(),
            )
            if (firstIndex.isNaN()) return@snapshotFlow null

            val itemsVisible = visibleItemsInfo.floatSumOf { itemInfo ->
                itemVisibilityPercentage(
                    itemSize = orientation.valueOf(itemInfo.size),
                    itemStartOffset = orientation.valueOf(itemInfo.offset),
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                    viewportEndOffset = layoutInfo.viewportEndOffset,
                )
            }

            val numItemsMin1 = numItems.fastCoerceAtLeast(1)
            scrollbarStateValue(
                thumbSizePercent = min(itemsVisible / numItemsMin1, 1f),
                thumbMovedPercent = min(firstIndex / numItemsMin1, 1f),
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state.onScroll(it) }
    }
}

private inline fun <T> List<T>.floatSumOf(
    selector: (T) -> Float,
) = fold(initial = 0f) { accumulator, element ->
    accumulator + selector(element)
}
