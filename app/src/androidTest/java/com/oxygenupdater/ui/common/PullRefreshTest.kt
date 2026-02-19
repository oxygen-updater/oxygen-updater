package com.oxygenupdater.ui.common

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.validateColumnLayout
import org.junit.Test

class PullRefreshTest : ComposeBaseTest() {

    @Test
    fun pullRefresh_initialLoadingState() {
        setContent(true, null) {
            @Suppress("SENSELESS_COMPARISON")
            it == null
        }

        rule[PullRefresh_ProgressIndicatorTestTag].validateColumnLayout(2).run {
            get(0).assert(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            get(1).assertHasTextExactly(R.string.loading)
        }
        rule[PullRefresh_ContentTestTag].assertDoesNotExist()
        rule[PullRefresh_IndicatorContainerTestTag].assertDoesNotExist()
    }

    @Test
    fun pullRefresh_manualRefresh() {
        setContent(false, listOf(1)) { it.isEmpty() }

        rule[PullRefresh_ProgressIndicatorTestTag].assertDoesNotExist()
        rule[PullRefresh_ContentTestTag].performTouchInput {
            swipeDown()
        }

        // Make sure pull refresh indicator shows up after swiping down
        rule[PullRefresh_IndicatorContainerTestTag].run {
            assertWidthIsAtLeast(1.dp)
            assertHeightIsAtLeast(1.dp)
        }
    }

    private fun <T> setContent(
        refreshing: Boolean,
        data: T,
        shouldShowProgressIndicator: (data: T) -> Boolean,
    ) = setContent {
        PullRefresh(
            state = RefreshAwareState(refreshing, data),
            shouldShowProgressIndicator = shouldShowProgressIndicator,
            onRefresh = {},
        ) {}
    }
}
