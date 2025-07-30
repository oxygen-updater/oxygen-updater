package com.oxygenupdater.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.LineHeightForTextStyle
import com.oxygenupdater.assertSizeIsEqualTo
import com.oxygenupdater.get
import com.oxygenupdater.icons.Android
import com.oxygenupdater.icons.Check
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.validateRowLayout
import org.junit.Test

class IconTextTest : ComposeBaseTest() {

    @Test
    fun iconText() {
        val text = "Text"
        var content by mutableStateOf<(@Composable RowScope.() -> Unit)?>(null)
        setContent {
            IconText(
                icon = Symbols.Android,
                text = text,
                maxLines = 1,
                content = content,
            )
        }

        val children = rule[IconTextTestTag].validateRowLayout(2)

        // Icon
        children[0].assertSizeIsEqualTo(24.dp)

        // Text
        children[1].run {
            assertHasTextExactly(text)
            fetchSemanticsNode().assertMaxLines(LineHeightForTextStyle.bodyMedium)
        }

        // First we test for the initial null value of content
        rule[ContentTestTag].assertDoesNotExist()

        // Then for non-null
        content = {
            Icon(
                imageVector = Symbols.Check,
                contentDescription = null,
                modifier = Modifier.testTag(ContentTestTag)
            )
        }
        rule[ContentTestTag].assertExists()
    }

    companion object {
        private const val ContentTestTag = "content"
    }
}
