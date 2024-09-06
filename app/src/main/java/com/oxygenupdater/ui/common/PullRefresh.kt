package com.oxygenupdater.ui.common

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ui.RefreshAwareState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PullRefresh(
    state: RefreshAwareState<T>,
    shouldShowProgressIndicator: (data: T) -> Boolean,
    onRefresh: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val refreshing = state.refreshing
    if (refreshing && shouldShowProgressIndicator(state.data)) Box(
        contentAlignment = Alignment.Center,
        modifier = modifierMaxSize.testTag(PullRefresh_ProgressIndicatorTestTag)
    ) {
        CircularProgressIndicator(Modifier.size(64.dp), strokeWidth = 6.dp)
    } else rememberPullToRefreshState().let {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            state = it,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = it,
                    isRefreshing = refreshing,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .testTag(PullRefresh_IndicatorContainerTestTag)
                )
            },
            content = content,
            modifier = Modifier.testTag(PullRefresh_ContentTestTag)
        )
    }
}

private const val TAG = "PullRefresh"

@VisibleForTesting
const val PullRefresh_ProgressIndicatorTestTag = TAG + "_ProgressIndicator"

@VisibleForTesting
const val PullRefresh_ContentTestTag = TAG + "_Content"

@VisibleForTesting
const val PullRefresh_IndicatorContainerTestTag = TAG + "_IndicatorContainer"
