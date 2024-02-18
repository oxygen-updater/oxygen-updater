package com.oxygenupdater.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.get
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.validateColumnLayout
import com.oxygenupdater.validateRowLayout
import org.junit.Before
import org.junit.Test

class ErrorStateTest : ComposeBaseTest() {

    private val trackOnRefreshClick = { trackCallback("onRefreshClick") }

    private var rich by mutableStateOf(true)
    private var onRefreshClick by mutableStateOf<(() -> Unit)?>(null)

    @Before
    fun setup() {
        rich = true
        onRefreshClick = trackOnRefreshClick
    }

    @Test
    fun errorState_bottomBar() {
        setContent(NavType.BottomBar) // column layout

        // This will also check if root node is a column
        validateNodes(
            column = rule[ErrorStateTestTag],
            expectedChildren = 4, // title, icon, text, button
        )
    }

    @Test
    fun errorState_sideRail() {
        setContent(NavType.SideRail) // row layout; scrollable column on the right

        // Ensure row layout (icon | title, text, button)
        val children = rule[ErrorStateTestTag].validateRowLayout(2)

        // This will also check if row's second child is a column
        validateNodes(
            column = children[1].assertHasScrollAction(),
            expectedChildren = 3, // title, text, button
        )
    }

    private fun validateNodes(
        column: SemanticsNodeInteraction,
        expectedChildren: Int,
    ) {
        // Check nodes that are always shown
        rule[ErrorState_IconTestTag].assertExists()
        rule[ErrorState_TitleTestTag].assertExists()

        // First we test for initial values of `rich = true` and non-null onRefreshClick
        rule[RichText_ContainerTestTag].assertExists()
        rule[ErrorState_TextTestTag].assertDoesNotExist()
        rule[OutlinedIconButtonTestTag].run {
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onRefreshClick")
        }
        column.validateColumnLayout(expectedChildren)

        // Then for normal text (rich = false)
        rich = false
        rule[RichText_ContainerTestTag].assertDoesNotExist()
        rule[ErrorState_TextTestTag].assertExists()
        column.validateColumnLayout(expectedChildren)

        // Then for button not being shown (onRefreshClick = null)
        onRefreshClick = null
        rule[OutlinedIconButtonTestTag].assertDoesNotExist()
    }

    private fun setContent(navType: NavType) = setContent {
        ErrorState(
            navType = navType,
            titleResId = R.string.error_maintenance,
            rich = rich,
            onRefreshClick = onRefreshClick,
        )
    }
}
