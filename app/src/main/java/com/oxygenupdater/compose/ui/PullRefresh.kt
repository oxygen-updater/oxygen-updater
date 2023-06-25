package com.oxygenupdater.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
inline fun <T> PullRefresh(
    state: RefreshAwareState<T>,
    shouldShowProgressIndicator: (T) -> Boolean = { true },
    noinline onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    val (refreshing, data) = state
    if (refreshing && shouldShowProgressIndicator(data)) Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator(Modifier.size(64.dp), strokeWidth = 6.dp)
    } else rememberPullRefreshState(refreshing, onRefresh).let {
        Box(Modifier.pullRefresh(it)) {
            content()

            PullRefreshIndicator(
                refreshing, it,
                Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colors.primary,
            )
        }
    }
}
