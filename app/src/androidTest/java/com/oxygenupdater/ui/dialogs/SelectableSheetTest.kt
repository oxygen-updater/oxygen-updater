package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.get
import com.oxygenupdater.models.SelectableModel
import com.oxygenupdater.ui.settings.DeviceSettingsListConfig
import com.oxygenupdater.validateLazyColumn
import org.junit.Test

class SelectableSheetTest : ComposeBaseTest() {

    private val config = DeviceSettingsListConfig
    private val itemCount = config.list.size

    @Test
    fun selectableSheet_device() {
        val titleResId = R.string.onboarding_device_chooser_title
        val captionResId = R.string.onboarding_device_chooser_caption
        var selectedItem: SelectableModel? = null
        setContent {
            Column {
                SelectableSheet(
                    hide = { trackCallback("hide") },
                    config = config,
                    titleResId = titleResId,
                    captionResId = captionResId,
                    onClick = { selectedItem = it },
                )
            }
        }

        rule[BottomSheet_HeaderTestTag].assertHasTextExactly(titleResId)
        rule[SelectableSheet_LazyColumnTestTag].validateLazyColumn(itemCount)

        rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag, true).run {
            repeat(itemCount) { index ->
                val item = config.list[index]
                val children = get(index).assertAndPerformClick().onChildren()
                assert(selectedItem == item) {
                    "Selected item did not match. Expected: $item, actual: $selectedItem."
                }

                ensureCallbackInvokedExactlyOnce("hide")

                // Icon (on) or Spacer (off)
                isToggleable().matches(children[0].fetchSemanticsNode())

                // Text & optional "initial" icon
                children[1].assertHasTextExactly(item.name)

                // "Recommended" icon
                children[2].run {
                    if (index == config.initialIndex) assertExists() else assertDoesNotExist()
                }
            }
        }

        rule[BottomSheet_CaptionTestTag].assertHasTextExactly(captionResId)
    }
}
