package com.oxygenupdater.ui.main

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.models.ServerStatus.Status
import com.oxygenupdater.ui.common.OutlinedIconButtonTestTag
import com.oxygenupdater.ui.common.OutlinedIconButton_TextTestTag
import com.oxygenupdater.ui.dialogs.AlertDialogTestTag
import com.oxygenupdater.ui.dialogs.AlertDialog_TextTestTag
import com.oxygenupdater.ui.dialogs.AlertDialog_TitleTestTag
import org.junit.Test

class ServerStatusDialogsTest : ComposeBaseTest() {

    private val root = rule[AlertDialogTestTag]

    private var status by mutableStateOf(Status.NORMAL)

    @Test
    fun serverStatusDialogs() {
        setContent {
            ServerStatusDialogs(
                status = status,
                openPlayStorePage = { trackCallback("openPlayStorePage") },
            )
        }

        status = Status.NORMAL
        root.assertDoesNotExist()
        status = Status.WARNING
        root.assertDoesNotExist()
        status = Status.ERROR
        root.assertDoesNotExist()
        status = Status.UNREACHABLE
        root.assertDoesNotExist()

        status = Status.MAINTENANCE
        validateForRecoverableErrors(
            titleResId = R.string.error_maintenance,
            textResId = R.string.error_maintenance_message,
        )

        status = Status.OUTDATED
        validateForRecoverableErrors(
            titleResId = R.string.error_app_outdated,
            textResId = R.string.error_app_outdated_message,
            confirmTextResId = R.string.error_google_play_button_text,
        )
    }

    private fun validateForRecoverableErrors(
        @StringRes titleResId: Int,
        @StringRes textResId: Int,
        @StringRes confirmTextResId: Int? = null,
    ) {
        root.onParent().assert(isDialog())
        rule[AlertDialog_TitleTestTag].assertHasTextExactly(titleResId)
        rule[AlertDialog_TextTestTag].assertHasTextExactly(textResId)
        if (confirmTextResId == null) {
            rule[OutlinedIconButtonTestTag].assertDoesNotExist()
        } else {
            rule[OutlinedIconButton_TextTestTag, true].assertHasTextExactly(confirmTextResId)
            rule[OutlinedIconButtonTestTag].run {
                performClick()
                ensureCallbackInvokedExactlyOnce("openPlayStorePage")
            }
        }
    }
}
