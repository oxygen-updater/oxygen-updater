package com.oxygenupdater.ui.install

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.RichText_ContainerTestTag
import com.oxygenupdater.validateLazyColumn
import org.junit.Test

class InstallGuideScreenTest : ComposeBaseTest() {

    private val data = PreviewInstallGuideScreenData
    private val itemCount = data.size

    @Test
    fun installGuideScreen() {
        var downloaded by mutableStateOf(false)
        setContent {
            InstallGuideScreen(
                state = RefreshAwareState(false, data),
                onRefresh = {},
                downloaded = downloaded,
            )
        }

        rule[InstallGuideScreen_LazyColumnTestTag].validateLazyColumn(
            itemCount + 1 // +1 for download instructions item
        )

        rule[InstallGuideScreen_InstructionsTestTag].run {
            // First we test for initial value of `downloaded = false`
            assertHasTextExactly(R.string.install_guide_download_instructions)
            // Then for true
            downloaded = true
            assertDoesNotExist()
        }

        val containerNodes = rule.onAllNodesWithTag(InstallGuideScreen_ItemRowTestTag).run {
            assertCountEquals(itemCount)
            assertAll(hasClickAction())
        }

        val titleNodes = rule.onAllNodesWithTag(InstallGuideScreen_ItemTitleTestTag, true)
        val subtitleNodes = rule.onAllNodesWithTag(InstallGuideScreen_ItemSubtitleTestTag, true)

        rule.onAllNodesWithTag(RichText_ContainerTestTag).run {
            /** Should not exist initially as it's wrapped in [com.oxygenupdater.ui.common.ExpandCollapse] */
            assertCountEquals(0)
        }

        repeat(itemCount) { index ->
            containerNodes[index].performClick() // should show RichText now
            titleNodes[index].assertHasTextExactly(data[index].title)
            subtitleNodes[index].assertHasTextExactly(data[index].subtitle)
        }

        rule.onAllNodesWithTag(RichText_ContainerTestTag).run {
            assertCountEquals(itemCount)
        }
    }
}
