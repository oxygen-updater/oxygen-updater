package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Column
import com.oxygenupdater.R
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.ui.theme.PreviewGetPrefBool
import org.junit.Test

class ContributorSheetTest : ModalBottomSheetTest() {

    @Test
    fun contributorSheet() {
        setContent {
            Column {
                ContributorSheet(
                    hide = {},
                    getPrefBool = PreviewGetPrefBool,
                    confirm = {},
                )
            }
        }

        validateHeaderContentCaption(
            headerResIdOrString = R.string.contribute_title,
            contentResIdOrString = R.string.contribute_explanation,
        ) {
            assertHasScrollAction()
        }

        /**
         * TODO(test/contributor): this can be tested only on rooted devices
         *  above API 29, because checkbox & buttons are guarded behind
         *  `ContributorUtils.isAtLeastQAndPossiblyRooted`.
         *  "PossiblyRooted" is determined by Shell.isAppGrantedRoot(),
         *  which is false for emulators and managed devices.
         */
        // First we test for the initial non-null value of `onConfirmClick`
        // composeTestRule[CheckboxText_CheckboxTestTag].assertExists()
        // composeTestRule[CheckboxText_TextTestTag].assertExists()
        // validateButtons(
        //     dismissResId = android.R.string.cancel,
        //     confirmResId = R.string.contribute_save,
        //     hidden = { hidden },
        //     result = { result },
        //     resetHidden = { hidden = false },
        // )
        //
        // // Then for null
        // onConfirmClick = null
        // composeTestRule[CheckboxText_CheckboxTestTag].assertDoesNotExist()
        // composeTestRule[CheckboxText_TextTestTag].assertDoesNotExist()
        // composeTestRule[BottomSheet_DismissButtonTestTag].assertDoesNotExist()
        // composeTestRule[OutlinedIconButtonTestTag].assertDoesNotExist()
    }
}
