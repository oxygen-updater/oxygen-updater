package com.oxygenupdater.ui.dialogs

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.RichTextType
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.update.DownloadErrorParams

@Composable
fun DownloadErrorSheet(hide: () -> Unit, params: DownloadErrorParams) {
    SheetHeader(R.string.download_error)

    val (text, type, resumable, callback) = params
    if (type == null) Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .testTag(BottomSheet_ContentTestTag)
    ) else RichText(
        text = text,
        type = type,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .testTag(DownloadErrorSheet_RichContentTestTag)
    )

    SheetButtons(
        dismissResId = R.string.download_error_close,
        onDismiss = hide,
        confirmIcon = if (resumable) Icons.Rounded.Download else Icons.Rounded.Autorenew,
        confirmResId = if (resumable) R.string.download_error_resume else R.string.download_error_retry,
        onConfirm = if (callback == null) null else ({ callback(resumable); hide() }),
    )
}

private const val TAG = "DownloadErrorSheet"

@VisibleForTesting
const val DownloadErrorSheet_RichContentTestTag = TAG + "_RichContent"

@PreviewThemes
@Composable
fun PreviewDownloadErrorUnsuccessfulResponseSheet() = PreviewModalBottomSheet {
    DownloadErrorSheet(
        hide = {},
        params = DownloadErrorParams(
            text = stringResource(
                R.string.download_error_unsuccessful_http_code_message, 404, "Not Found",
            ) + stringResource(
                R.string.download_error_unsuccessful_explanation_file_removed
            ) + "\n\n" + stringResource(
                R.string.download_error_unsuccessful_suffix
            ),
            type = RichTextType.Html,
        )
    )
}

@PreviewThemes
@Composable
fun PreviewDownloadErrorServerSheet() = PreviewModalBottomSheet {
    DownloadErrorSheet(
        hide = {},
        params = DownloadErrorParams(stringResource(R.string.download_error_server), resumable = true) {}
    )
}

@PreviewThemes
@Composable
fun PreviewDownloadErrorStorageSheet() = PreviewModalBottomSheet {
    DownloadErrorSheet(
        hide = {},
        params = DownloadErrorParams(stringResource(R.string.download_error_storage))
    )
}
