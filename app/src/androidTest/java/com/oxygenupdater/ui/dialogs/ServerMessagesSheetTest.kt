package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.ui.common.RichText_ContainerTestTag
import com.oxygenupdater.validateLazyColumn
import com.oxygenupdater.validateRowLayout
import org.junit.Test

class ServerMessagesSheetTest : ComposeBaseTest() {

    private val itemCount = PreviewServerMessagesList.size

    @Test
    fun serverMessagesSheet() {
        setContent {
            Column {
                ServerMessagesSheet(PreviewServerMessagesList)
            }
        }

        rule[BottomSheet_HeaderTestTag].assertHasTextExactly(R.string.settings_push_from_server)

        rule[ServerMessagesSheet_LazyColumnTestTag].validateLazyColumn(itemCount)

        rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag).run {
            repeat(itemCount) { get(it).validateRowLayout(2) }
        }

        rule.onAllNodesWithTag(RichText_ContainerTestTag).run {
            assertCountEquals(itemCount)
        }
    }
}
