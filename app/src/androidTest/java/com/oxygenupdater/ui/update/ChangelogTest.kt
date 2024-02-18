package com.oxygenupdater.ui.update

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.common.RichText_ContainerTestTag
import com.oxygenupdater.utils.UpdateDataVersionFormatter
import com.oxygenupdater.validateColumnLayout
import org.junit.Test

class ChangelogTest : ComposeBaseTest() {

    private val richText = rule[RichText_ContainerTestTag]
    private val placeholderText = rule[Changelog_PlaceholderTestTag]

    @Test
    fun changelog() {
        var updateData by mutableStateOf(PreviewUpdateData)
        var isDifferentVersion by mutableStateOf(true)
        var showAdvancedModeTip by mutableStateOf(true)
        setContent {
            ChangelogContainer(
                refreshing = false,
                updateData = updateData,
                isDifferentVersion = isDifferentVersion,
                showAdvancedModeTip = showAdvancedModeTip,
                getPrefStr = GetPrefStrForUpdateMethod,
            ) { Box(Modifier.testTag(ContentTestTag)) }
        }

        rule[Changelog_ContainerTestTag].validateColumnLayout(3).run {
            onLast().assert(hasTestTag(ContentTestTag))
            onFirst().assert(hasTestTag(Changelog_DifferentVersionNoticeTestTag))
        }

        rule[Changelog_DifferentVersionNoticeTestTag].run {
            val notice = activity.getString(
                R.string.update_information_different_version_changelog_notice_base,
                UpdateDataVersionFormatter.getFormattedVersionNumber(PreviewUpdateData),
                UpdateMethod,
            )

            // First we test for the initial boolean values of true
            assertHasTextExactly(
                notice + activity.getString(R.string.update_information_different_version_changelog_notice_advanced),
            )

            // Then for `showAdvancedModeTip = false`
            showAdvancedModeTip = false; assertHasTextExactly(notice)

            // Then for `isDifferentVersion = false`
            isDifferentVersion = false; assertDoesNotExist()
        }

        /** First we test for the initial value of [PreviewUpdateData] */
        richText.assertExists()
        placeholderText.assertDoesNotExist()

        // Then for null/blank changelog & description
        updateData = UpdateData(id = 1, changelog = null, description = null) // null
        validateForEmptyChangelogOrDescription()
        updateData = UpdateData(id = 1, changelog = "", description = "") // empty
        validateForEmptyChangelogOrDescription()
        updateData = UpdateData(id = 1, changelog = " \t\n", description = " \t\n") // blank
        validateForEmptyChangelogOrDescription()
    }

    private fun validateForEmptyChangelogOrDescription() {
        richText.assertDoesNotExist()
        placeholderText.assertHasTextExactly(R.string.update_information_description_not_available)
    }

    companion object {
        private const val ContentTestTag = "content"
    }
}
