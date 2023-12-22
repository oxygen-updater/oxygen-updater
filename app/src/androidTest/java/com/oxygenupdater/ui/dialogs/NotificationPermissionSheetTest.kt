package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Column
import com.oxygenupdater.R
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.get
import com.oxygenupdater.ui.common.CheckboxText_CheckboxTestTag
import com.oxygenupdater.ui.common.CheckboxText_TextTestTag
import org.junit.Test

class NotificationPermissionSheetTest : ModalBottomSheetTest() {

    @Test
    fun notificationPermissionSheet() {
        var hidden = false
        var result = false
        var launchClicked = false
        setContent {
            Column {
                NotificationPermissionSheet(
                    hide = { hidden = true; result = it },
                    launchPermissionRequest = { launchClicked = true },
                )
            }
        }

        validateHeaderContentCaption(
            headerResIdOrString = R.string.notification_permission_title,
            contentResIdOrString = R.string.notification_permission_text,
        ) {
            assertHasScrollAction()
        }

        rule[CheckboxText_CheckboxTestTag].assertExists()
        rule[CheckboxText_TextTestTag].assertHasTextExactly(R.string.do_not_show_again_checkbox)

        val dismissButton = validateDismissButton(
            dismissResId = android.R.string.cancel,
            hidden = { hidden },
            result = { result }, // we don't really have a conventional result here
        )

        // Then for non-null
        hidden = false // reset

        validateConfirmButton(
            dismissButton = dismissButton,
            confirmResId = android.R.string.ok,
            hidden = { !hidden }, // clicking confirm should not hide the sheet
            result = { launchClicked },
            resultFailureMessage = { "launchPermissionRequest() was not invoked" },
        )
    }
}
