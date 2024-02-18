package com.oxygenupdater.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.LineHeightForTextStyle
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.get
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class AppBarTest : ComposeBaseTest() {

    @Test
    fun topAppBar() {
        var subtitleResId by mutableIntStateOf(0)
        var onNavIconClick by mutableStateOf<(() -> Unit)?>(null)
        var actions by mutableStateOf<@Composable (RowScope.() -> Unit)?>(null)
        setContent {
            TopAppBar(
                scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                subtitleResId = subtitleResId,
                root = false,
                onNavIconClick = onNavIconClick,
                actions = actions,
            )
        }

        // First we test for the initial null value of `onNavIconClick`
        rule[AppBar_IconButtonTestTag].assertDoesNotExist()

        // Then for non-null
        onNavIconClick = { trackCallback("onNavIconClick") }
        rule[AppBar_IconButtonTestTag].run {
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onNavIconClick")
        }

        // First we test for the initial null value of action
        rule[AppBar_ActionsTestTag].assertDoesNotExist()
        // Then for non-null
        actions = { Box(Modifier.testTag(AppBar_ActionsTestTag)) }
        rule[AppBar_ActionsTestTag].assertExists()

        rule[AppBar_TitleTestTag].run {
            assertHasTextExactly(R.string.app_name)
            fetchSemanticsNode().assertMaxLines(LineHeightForTextStyle.titleLarge)
        }

        rule[AppBar_SubtitleTestTag].run {
            assertHasTextExactly("v${BuildConfig.VERSION_NAME}")
            subtitleResId = R.string.settings; advanceFrame()
            assertHasTextExactly(subtitleResId)
            fetchSemanticsNode().assertMaxLines(LineHeightForTextStyle.bodyMedium)
        }
    }

    @Test
    fun collapsingAppBar() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior

        val title = "An unnecessarily long news title, to get an accurate understanding of how long titles are rendered"
        var subtitle by mutableStateOf<String?>(null)
        var onNavIconClick by mutableStateOf<(() -> Unit)?>(null)
        setContent {
            scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            CollapsingAppBar(
                scrollBehavior = scrollBehavior,
                image = {},
                title = title,
                subtitle = subtitle,
                onNavIconClick = onNavIconClick,
            )
        }

        rule[CollapsingAppBar_ImageTestTag].run {
            val bounds = rule.onRoot().getUnclippedBoundsInRoot()
            assertWidthIsEqualTo(bounds.width)
            assertHeightIsEqualTo(bounds.height)
        }

        rule[AppBar_SubtitleTestTag].run {
            // First we test for the initial null value of subtitle
            assertDoesNotExist()
            // Then for non-null
            subtitle = "An unnecessarily long news subtitle, to get an accurate understanding of how long subtitles are rendered"
            assertHasTextExactly(subtitle)
            fetchSemanticsNode().assertMaxLines(LineHeightForTextStyle.bodyLarge)
        }

        // First we test for the initial null value of onNavIconClick
        rule[AppBar_IconButtonTestTag].assertDoesNotExist()
        rule[AppBar_TitleTestTag, true].assertXPositionExactly(16.dp)
        rule[AppBar_SubtitleTestTag, true].assertXPositionExactly(16.dp)

        // Then for non-null
        onNavIconClick = { trackCallback("onNavIconClick") }
        rule[AppBar_TitleTestTag, true].assertXPositionExactly(20.dp)
        rule[AppBar_SubtitleTestTag, true].assertXPositionExactly(20.dp)
        rule[AppBar_IconButtonTestTag].run {
            assertHeightIsAtLeast(CollapsingAppBarHeight.first)
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onNavIconClick")
        }

        // First we test for the expanded state
        rule[AppBar_TitleTestTag, true].run {
            assertHasTextExactly(title)
            fetchSemanticsNode().assertMaxLines(
                lineHeight = LineHeightForTextStyle.headlineSmall,
                expectedMaxLines = 4,
            )
        }

        // Then for collapsed
        rule[CollapsingAppBarTestTag].performTouchInput { swipeUp() }
        rule[AppBar_SubtitleTestTag, true].assertXPositionExactly(56.dp)
        rule[AppBar_TitleTestTag, true].fetchSemanticsNode().run {
            assertXPositionExactly(56.dp)
            assertMaxLines(
                lineHeight = LineHeightForTextStyle.headlineSmall,
                expectedMaxLines = 1,
            )
        }

        // Ensure tooltip shows when long-pressing
        rule[CollapsingAppBar_TooltipBoxTestTag].performTouchInput { longClick() }
        rule[CollapsingAppBar_TooltipTestTag].run {
            assertIsDisplayed()
            onChildren().assertAny(hasTextExactly(title))

            // Test if pressing the back button hides the tooltip
            activity.onBackPressedDispatcher.onBackPressed()
            assertIsNotDisplayed()
        }
    }

    private fun SemanticsNodeInteraction.assertXPositionExactly(
        dp: Dp,
    ) = fetchSemanticsNode().assertXPositionExactly(dp)

    private fun SemanticsNode.assertXPositionExactly(dp: Dp) = with(rule.density) {
        positionInRoot.x.toDp().assertIsEqualTo(dp, "X position")
    }

    companion object {
        private const val AppBar_ActionsTestTag = "AppBar_Actions"
    }
}
