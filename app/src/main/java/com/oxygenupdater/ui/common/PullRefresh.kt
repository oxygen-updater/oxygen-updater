package com.oxygenupdater.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.pullrefresh.PullRefreshIndicator
import com.oxygenupdater.ui.pullrefresh.pullRefresh
import com.oxygenupdater.ui.pullrefresh.rememberPullRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PullRefresh(
    state: RefreshAwareState<T>,
    shouldShowProgressIndicator: (data: T) -> Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    val refreshing = state.refreshing
    if (refreshing && shouldShowProgressIndicator(state.data)) Box(modifierMaxSize, Alignment.Center) {
        CircularProgressIndicator(Modifier.size(64.dp), strokeWidth = 6.dp)
    } else rememberPullRefreshState(refreshing, onRefresh).let {
        Box(Modifier.pullRefresh(it)) {
            content()

            PullRefreshIndicator(
                refreshing = refreshing,
                state = it,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
