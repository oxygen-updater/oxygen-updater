package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.performTextInput
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.get
import com.oxygenupdater.models.SelectableModel
import com.oxygenupdater.ui.settings.DeviceSearchFilter
import com.oxygenupdater.ui.settings.DeviceSettingsListConfig
import com.oxygenupdater.validateLazyColumn
import org.junit.Test

class SelectableSheetTest : ComposeBaseTest() {

    private val config = DeviceSettingsListConfig
    private val itemCount = config.list.size

    @Test
    fun selectableSheet_device() {
        val titleResId = R.string.settings_search_devices
        val captionResId = R.string.onboarding_device_chooser_caption
        var selectedItem: SelectableModel? = null
        setContent {
            Column {
                SelectableSheet(
                    hide = { trackCallback("hide") },
                    config = config,
                    titleResId = titleResId,
                    captionResId = captionResId,
                    filter = DeviceSearchFilter,
                    onClick = { selectedItem = it },
                )
            }
        }

        rule[BottomSheet_HeaderTestTag].assertDoesNotExist()
        rule[SelectableSheet_SearchBarFieldTestTag].assertHasTextExactly(titleResId)
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

                children[1].assertHasTextExactly(item.name) // text
                children[2].run { // optional "recommended" icon
                    if (item.id == config.recommendedId) assertExists() else assertDoesNotExist()
                }
            }
        }

        rule[BottomSheet_CaptionTestTag].assertHasTextExactly(captionResId)

        // Now, test search bar functionality
        val recommendedItem = DeviceSettingsListConfig.list.findLast {
            it.id == DeviceSettingsListConfig.recommendedId
        }!!

        rule[SelectableSheet_SearchBarTestTag].assertExists()
        rule[SelectableSheet_SearchBarFieldTestTag].run {
            assert(
                // Test only the semantics we care about
                SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)
                    // Must be editable
                    .and(SemanticsMatcher.expectValue(SemanticsProperties.IsEditable, true))
                    // Must not be a password field
                    .and(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password))
            )

            assertAndPerformClick() // should be focused now
            // After entering the name of the recommended device…
            recommendedItem.name!!.also {
                performTextInput(it); assert(hasTextExactly(it, includeEditableText = true))
            }
        }

        // Wait for debounce to complete
        @OptIn(ExperimentalTestApi::class)
        rule.waitUntilExactlyOneExists(hasTestTag(BottomSheet_ItemRowTestTag), 500)

        // …the list should only have 1 child, and it should be the recommended device
        rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag, true).run {
            assertCountEquals(1)

            val children = get(0).onChildren()
            children[1].assertHasTextExactly(recommendedItem.name) // text
            children[2].assertExists() // "recommended" icon
        }
    }
}
