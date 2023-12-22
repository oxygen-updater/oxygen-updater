package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.ui.common.RichTextType
import com.oxygenupdater.ui.update.DownloadErrorParams
import org.junit.Test

class DownloadErrorSheetTest : ModalBottomSheetTest() {

    @Test
    fun downloadErrorSheet() {
        val text = "Text"
        var hidden = false
        var params by mutableStateOf(DownloadErrorParams(text = text))
        setContent {
            Column {
                DownloadErrorSheet(
                    hide = { hidden = true },
                    params = params,
                )
            }
        }

        // First we test for the initial value of params (null `type`)
        validateHeaderContentCaption(
            headerResIdOrString = R.string.download_error,
            contentResIdOrString = params.text,
        )

        // Then for custom type
        params = DownloadErrorParams(text = text, type = RichTextType.Custom)
        validateContentNodesForRichTextType()

        // Then for HTML type
        params = DownloadErrorParams(text = text, type = RichTextType.Html)
        validateContentNodesForRichTextType()

        // Then for Markdown type
        params = DownloadErrorParams(text = text, type = RichTextType.Markdown)
        validateContentNodesForRichTextType()

        validateButtons(
            dismissResId = R.string.download_error_close,
            confirmResId = if (params.callback == null) null else {
                if (params.resumable) R.string.download_error_resume else R.string.download_error_retry
            },
            hidden = { hidden },
            result = { params.resumable },
            resetHidden = { hidden = false },
        )
    }

    private fun validateContentNodesForRichTextType() {
        rule[BottomSheet_ContentTestTag].assertDoesNotExist()
        rule[DownloadErrorSheet_RichContentTestTag].assertExists()
    }
}
