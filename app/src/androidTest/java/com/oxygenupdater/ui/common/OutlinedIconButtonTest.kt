package com.oxygenupdater.ui.common

import androidx.compose.ui.test.onParent
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertSizeIsEqualTo
import com.oxygenupdater.get
import com.oxygenupdater.icons.Android
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.validateRowLayout
import org.junit.Test

class OutlinedIconButtonTest : ComposeBaseTest() {

    @Test
    fun outlinedIconButton() {
        setContent {
            OutlinedIconButton(
                onClick = { trackCallback("onClick") },
                icon = Symbols.Android,
                textResId = R.string.app_name,
            )
        }

        rule[OutlinedIconButtonTestTag].run {
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onClick")
        }

        // Icon
        rule[OutlinedIconButton_IconTestTag, true].run {
            assertSizeIsEqualTo(18.dp)
            onParent().validateRowLayout(2)
        }

        // Text
        rule[OutlinedIconButton_TextTestTag, true].run {
            assertHasTextExactly(R.string.app_name)
            onParent().validateRowLayout(2)
        }
    }
}
