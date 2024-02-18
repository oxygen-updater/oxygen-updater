package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Column
import com.oxygenupdater.R
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.validateColumnLayout
import org.junit.Test

class AdvancedModeSheetTest : ModalBottomSheetTest() {

    @Test
    fun advancedModeSheet() {
        var result: Boolean? = null
        setContent {
            Column {
                AdvancedModeSheet { result = it }
            }
        }

        validateHeaderContentCaption(
            headerResIdOrString = R.string.settings_advanced_mode,
            contentResIdOrString = null,
            captionResIdOrString = R.string.settings_advanced_mode_caption,
        ) {
            assertHasScrollAction()
            val children = validateColumnLayout(2)
            children[0].assertHasTextExactly(R.string.settings_advanced_mode_explanation)
            children[1].assertHasTextExactly(R.string.settings_advanced_mode_uses)
        }

        validateButtons(
            dismissResId = android.R.string.cancel,
            confirmResId = R.string.enable,
            result = { result },
        )
    }
}
