package com.oxygenupdater.ui.common

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ui.RefreshAwareState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PullRefresh(
    state: RefreshAwareState<T>,
    shouldShowProgressIndicator: (data: T) -> Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    val refreshing = state.refreshing
    if (refreshing && shouldShowProgressIndicator(state.data)) Box(
        contentAlignment = Alignment.Center,
        modifier = modifierMaxSize.testTag(PullRefresh_ProgressIndicatorTestTag)
    ) {
        CircularProgressIndicator(Modifier.size(64.dp), strokeWidth = 6.dp)
    } else rememberPullToRefreshState().let {
        /**
         * If our own refresh [state] is false, M3's PullToRefreshState needs to
         * be signalled. Note: we're deliberately not handling the `true` case,
         * i.e. call `startRefresh`, because that would fire off [onRefresh].
         *
         * We don't want that, because ViewModels already fetch data on init,
         * and we'd get a duplicate network request if we do it here too.
         *
         * The downside is that indicator isn't shown initially.
         */
        if (!refreshing) LaunchedEffect(Unit) { it.endRefresh() }
        if (it.isRefreshing) LaunchedEffect(Unit) { onRefresh() }

        Box(
            Modifier
                .nestedScroll(it.nestedScrollConnection)
                .testTag(PullRefresh_ContentTestTag)
        ) {
            content()

            PullToRefreshContainer(
                state = it,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        if (it.isRefreshing) return@graphicsLayer

                        // Grow when pulling down. This is required, otherwise the indicator
                        // would always be visible even when not refreshing.
                        val scale = LinearOutSlowInEasing
                            .transform(it.progress)
                            .coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                    }
                    .testTag(PullRefresh_IndicatorContainerTestTag)
            )
        }
    }
}

private const val TAG = "PullRefresh"

@VisibleForTesting
const val PullRefresh_ProgressIndicatorTestTag = TAG + "_ProgressIndicator"

@VisibleForTesting
const val PullRefresh_ContentTestTag = TAG + "_Content"

@VisibleForTesting
const val PullRefresh_IndicatorContainerTestTag = TAG + "_IndicatorContainer"
