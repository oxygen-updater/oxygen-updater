package com.oxygenupdater.ui.device

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onParent
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.ui.dialogs.AlertDialogTestTag
import com.oxygenupdater.ui.dialogs.AlertDialog_TextTestTag
import org.junit.Test

class IncorrectDeviceDialogTest : ComposeBaseTest() {

    @Test
    fun incorrectDeviceDialog() {
        var mismatchStatus by mutableStateOf(Triple(false, "correct", "correct"))
        setContent {
            IncorrectDeviceDialog(
                hide = {},
                mismatchStatus = mismatchStatus,
            )
        }

        // First we test for the initial value of false
        rule[AlertDialogTestTag].assertDoesNotExist()

        // Then for true
        mismatchStatus = Triple(true, "incorrect", "correct")
        rule[AlertDialogTestTag].run {
            assertExists()
            onParent().assert(isDialog())
        }
        rule[AlertDialog_TextTestTag].assertHasTextExactly(
            activity.getString(R.string.incorrect_device_warning_message, mismatchStatus.second, mismatchStatus.third),
        )
    }
}
