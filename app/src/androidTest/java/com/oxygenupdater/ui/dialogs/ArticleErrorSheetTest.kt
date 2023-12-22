package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Column
import com.oxygenupdater.R
import com.oxygenupdater.assertHasScrollAction
import org.junit.Test

class ArticleErrorSheetTest : ModalBottomSheetTest() {

    @Test
    fun articleErrorSheet() {
        val title = "Title"
        var hidden = false
        var result = false
        setContent {
            Column {
                ArticleErrorSheet(
                    hide = { hidden = true },
                    title = title,
                    confirm = { result = true },
                )
            }
        }

        validateHeaderContentCaption(
            headerResIdOrString = title,
            contentResIdOrString = R.string.news_load_network_error,
        ) {
            assertHasScrollAction()
        }

        validateButtons(
            dismissResId = R.string.download_error_close,
            confirmResId = R.string.download_error_retry,
            hidden = { hidden },
            result = { result },
            resetHidden = { hidden = false },
        )
    }
}
