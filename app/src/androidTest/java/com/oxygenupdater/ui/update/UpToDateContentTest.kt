package com.oxygenupdater.ui.update

import android.content.pm.ActivityInfo
import android.os.Build.UNKNOWN
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasNoScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.get
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.common.IconTextTestTag
import com.oxygenupdater.ui.common.RichText_ContainerTestTag
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.utils.UpdateDataVersionFormatter
import com.oxygenupdater.validateColumnLayout
import org.junit.Test

class UpToDateContentTest : ComposeBaseTest() {

    private val iconTextNodes = rule.onAllNodesWithTag(IconTextTestTag)

    @Test
    fun upToDateContent_expanded() {
        /**
         * "Normal" devices (i.e. not tablets or foldables) may not have enough
         * horizontal space in portrait mode, so we're forcing landscape.
         * This is necessary because we compare bounds of the info column and
         * ChangelogContainer towards the end of this test.
         *
         * Info column uses [androidx.compose.foundation.layout.IntrinsicSize.Max],
         * so it may occupy the entire screen's width if the largest item requires
         * it. This means that ChangelogContainer's bounds would be 0, which would
         * fail assertions (it's not on the screen at all).
         */
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        setContent(WindowWidthSizeClass.Expanded)
        rule[UpToDateContentTestTag].assertExists()

        // AdvancedModeTip
        iconTextNodes.onFirst().onChildAt(1).assertHasTextExactly(
            activity.getString(R.string.update_information_banner_advanced_mode_tip, UpdateMethod),
        )

        // Info column (left side)
        val children = rule[UpToDateContent_InfoColumnTestTag].run {
            assertHasScrollAction()
            onChildren()
        }.also {
            it.ensureCountIncludingSoftwareItems(7)
        }

        children[0].run {
            assert(hasTestTag(IconTextTestTag))
            onChildAt(1).assertHasTextExactly(R.string.update_information_system_is_up_to_date)
        }

        // Changelog (right side)
        validateChangelogContainer(true)

        val (lLeft, lTop, lRight, lBottom) = rule[UpToDateContent_InfoColumnTestTag].run {
            fetchSemanticsNode().boundsInRoot
        }
        val (_, rTop, rRight, rBottom) = rule[Changelog_ContainerTestTag].run {
            fetchSemanticsNode().boundsInRoot
        }

        // Left column must be all the way to the left
        assert(lLeft == 0f) {
            "$UpToDateContent_InfoColumnTestTag bounds must have l=0 (actual: ${lLeft})"
        }

        // Changelog container must be to the right of the left column
        assert(rRight > lRight) {
            "$Changelog_ContainerTestTag bounds must have r>${lRight} (actual: ${rRight})"
        }
        // Changelog container must have the same top & bottom bounds as the left column
        assert(rTop == lTop) {
            "$Changelog_ContainerTestTag bounds must have t=${lTop} (actual: ${rTop})"
        }
        assert(rBottom == lBottom) {
            "$Changelog_ContainerTestTag bounds must have b=${lBottom} (actual: ${rBottom})"
        }
    }

    @Test
    fun upToDateContent_compact() {
        setContent(WindowWidthSizeClass.Compact)

        val children = rule[UpToDateContentTestTag].run {
            assertHasScrollAction()
            onChildren()
        }.also {
            it.ensureCountIncludingSoftwareItems(10)
        }

        // AdvancedModeTip
        iconTextNodes.onFirst().onChildAt(1).assertHasTextExactly(
            activity.getString(R.string.update_information_banner_advanced_mode_tip, UpdateMethod),
        )

        children[1].run {
            assert(hasTestTag(IconTextTestTag))
            onChildAt(1).assertHasTextExactly(R.string.update_information_system_is_up_to_date)
        }

        iconTextNodes.onLast().run {
            assertHasTextExactly(R.string.update_information_view_update_information)

            rule[Changelog_ContainerTestTag].assertDoesNotExist()
            assertAndPerformClick() // should be visible now
            validateChangelogContainer(false)
        }
    }

    private fun SemanticsNodeInteractionCollection.ensureCountIncludingSoftwareItems(min: Int) {
        var count = min

        /**
         * There are 3 nodes per [com.oxygenupdater.ui.device.Item] (icon, title, subtitle).
         *
         * Visible only if not [UNKNOWN] (3):
         * - [R.string.device_information_oxygen_os_version]
         * - [R.string.device_information_oxygen_os_ota_version]
         * - [R.string.device_information_patch_level_version]
         *
         * Always visible (2):
         * - [R.string.device_information_os_version]
         * - [R.string.device_information_incremental_os_version]
         */
        if (SystemVersionProperties.oxygenOSVersion != UNKNOWN) count += 3
        if (SystemVersionProperties.oxygenOSOTAVersion != UNKNOWN) count += 3
        if (SystemVersionProperties.securityPatchDate != UNKNOWN) count += 3

        assertCountEquals(count)
    }

    private fun validateChangelogContainer(scrollable: Boolean) {
        rule[Changelog_PlaceholderTestTag].assertDoesNotExist()

        val children = rule[Changelog_ContainerTestTag].run {
            if (scrollable) assertHasScrollAction() else assert(hasNoScrollAction())
            validateColumnLayout(2)
        }

        // Conditional notice
        children[0].assertHasTextExactly(
            activity.getString(
                R.string.update_information_different_version_changelog_notice_base,
                UpdateDataVersionFormatter.getFormattedVersionNumber(PreviewUpdateData),
                UpdateMethod,
            ) + activity.getString(R.string.update_information_different_version_changelog_notice_advanced),
        )

        /** [com.oxygenupdater.models.UpdateData.description] via RichText */
        children[1].assert(hasTestTag(RichText_ContainerTestTag))
    }

    private fun setContent(windowWidthSize: WindowWidthSizeClass) = setContent {
        UpToDate(
            navType = NavType.BottomBar,
            windowWidthSize = windowWidthSize,
            refreshing = false,
            updateData = PreviewUpdateData,
            getPrefStr = GetPrefStrForUpdateMethod,
        )
    }
}
