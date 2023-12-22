package com.oxygenupdater.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.LineHeightForTextStyle
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.assertHasSemanticsKey
import com.oxygenupdater.get
import org.junit.Before
import org.junit.Test

class MainNavigationTest : ComposeBaseTest() {

    private val itemCount = MainScreens.size
    private var currentRoute by mutableStateOf(UpdateRoute)

    @Before
    fun setup() {
        currentRoute = UpdateRoute
    }

    @Test
    fun mainNavigation_bar() {
        setContent {
            MainNavigationBar(
                currentRoute = currentRoute,
                navigateTo = { currentRoute = it },
                setSubtitleResId = { trackCallback("setSubtitleResId: $it") },
            )
        }

        rule[MainNavigation_RailTestTag].assertDoesNotExist()
        rule[MainNavigation_BarTestTag].run {
            val child = onChild()
            child.assertHasSemanticsKey(SemanticsProperties.SelectableGroup)
            child.onChildren().validateItems()
        }

        validateLabelMaxLines()
    }

    @Test
    fun mainNavigation_rail() {
        setContent {
            MainNavigationRail(
                currentRoute = currentRoute,
                root = false,
                onNavIconClick = { trackCallback("onNavIconClick") },
                navigateTo = { currentRoute = it },
                setSubtitleResId = { trackCallback("setSubtitleResId: $it") },
            )
        }

        rule[MainNavigation_BarTestTag].assertDoesNotExist()
        rule[MainNavigation_RailTestTag].run {
            onChild().assertHasSemanticsKey(SemanticsProperties.SelectableGroup)
        }

        rule[MainNavigation_Rail_IconButtonTestTag].run {
            assertHeightIsEqualTo(64.dp)
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onNavIconClick")
        }

        rule[MainNavigation_Rail_LazyColumnTestTag].run {
            assertHasScrollAction()
            onChildren().validateItems()
        }

        validateLabelMaxLines()
    }

    private fun SemanticsNodeInteractionCollection.validateItems() = repeat(itemCount) { index ->
        val screen = MainScreens[index]
        val child = get(index)
        child.assertHasTextExactly(screen.labelResId)

        val route = screen.route
        if (route == currentRoute) child.assertIsSelected() else child.assertIsNotSelected()

        child.assertHasClickAction(); child.performClick(); advanceFrame() // should change currentRoute
        assert(route == currentRoute) {
            "Current route did not change. Expected: $route, actual: $currentRoute."
        }

        if (screen != Screen.Update) ensureCallbackInvokedExactlyOnce(
            "setSubtitleResId: ${if (screen.useVersionName) 0 else screen.labelResId}",
        ) else ensureNoCallbacksWereInvoked()
    }

    private fun validateLabelMaxLines() = rule.onAllNodesWithTag(
        MainNavigation_LabelTestTag, true
    ).fetchSemanticsNodes().fastForEach {
        it.assertMaxLines(LineHeightForTextStyle.labelMedium)
    }
}
