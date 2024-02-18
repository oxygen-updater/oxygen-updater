package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.get
import com.oxygenupdater.ui.Theme
import com.oxygenupdater.validateLazyColumn
import org.junit.Test

class ThemeSheetTest : ComposeBaseTest() {

    private val itemCount = Themes.size

    @Test
    fun themeSheet() {
        var selectedTheme: Theme? = null
        setContent {
            Column {
                ThemeSheet { selectedTheme = it }
            }
        }

        rule[BottomSheet_HeaderTestTag].assertHasTextExactly(R.string.label_theme)
        rule[ThemeSheet_LazyColumnTestTag].validateLazyColumn(itemCount)

        // unmerged tree => children will be laid out as Icon/Spacer, Text, Text
        // instead of Icon/Spacer, Column -> Text, Text
        rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag, true).run {
            repeat(itemCount) { index ->
                val theme = Themes[index]
                val children = get(index).assertAndPerformClick().onChildren()
                assert(selectedTheme == theme) {
                    "Selected theme did not match. Expected: $theme, actual: $selectedTheme."
                }

                // Icon (on) or Spacer (off)
                isToggleable().matches(children[0].fetchSemanticsNode())

                // Title & subtitle
                children[1].assertHasTextExactly(theme.titleResId)
                children[2].assertHasTextExactly(theme.subtitleResId)
            }
        }

        rule[BottomSheet_CaptionTestTag].assertHasTextExactly(R.string.theme_additional_note)
    }
}
