package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onParent
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.icons.Logos
import com.oxygenupdater.icons.PlayStore
import com.oxygenupdater.ui.common.OutlinedIconButtonTestTag
import org.junit.Test

class AlertDialogTest : ComposeBaseTest() {

    @Test
    fun alertDialog() {
        val text = "Text"
        var confirmIconAndResId by mutableStateOf<Pair<ImageVector, Int>?>(null)
        var content by mutableStateOf<(@Composable ColumnScope.() -> Unit)?>(null)
        setContent {
            AlertDialog(
                action = {},
                titleResId = R.string.app_name,
                text = text,
                confirmIconAndResId = confirmIconAndResId,
                content = content,
            )
        }

        rule[AlertDialogTestTag].run {
            assertExists()
            onParent().assert(isDialog())
        }
        rule[AlertDialog_TitleTestTag].assertHasTextExactly(R.string.app_name)
        rule[AlertDialog_TextTestTag].assertHasTextExactly(text)
        rule[AlertDialog_DismissButtonTestTag].assertExists()

        // First we test for the default null values of state variables
        rule[OutlinedIconButtonTestTag].assertDoesNotExist()
        rule[AlertDialog_ContentTestTag].assertDoesNotExist()

        // Then for non-null
        confirmIconAndResId = Logos.PlayStore to R.string.error_google_play_button_text
        content = {}
        rule[OutlinedIconButtonTestTag].assertExists()
        rule[AlertDialog_ContentTestTag].assertExists()
    }
}
