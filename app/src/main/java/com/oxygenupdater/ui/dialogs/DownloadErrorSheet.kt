package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.ui.common.OutlinedIconButton
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.RichTextType
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.update.DownloadErrorParams
import com.oxygenupdater.utils.LocalNotifications

@Composable
fun DownloadErrorSheet(hide: () -> Unit, params: DownloadErrorParams) {
    SheetHeader(R.string.download_error)

    val (text, type, resumable, callback) = params
    if (type == null) Text(
        text,
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        style = MaterialTheme.typography.bodyMedium
    ) else RichText(text, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), type = type)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton({
            LocalNotifications.hideDownloadCompleteNotification()
            hide()
        }, Modifier.padding(end = 8.dp), colors = textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text(stringResource(R.string.download_error_close))
        }

        if (callback == null) return@Row
        val icon = if (resumable) Icons.Rounded.Download else Icons.Rounded.Autorenew
        val resId = if (resumable) R.string.download_error_resume else R.string.download_error_retry
        OutlinedIconButton({
            LocalNotifications.hideDownloadCompleteNotification()
            hide()
            callback(resumable) // must be after `hide` so that the extra flag works correctly
        }, icon, resId)
    }
}

@PreviewThemes
@Composable
fun PreviewDownloadErrorUnsuccessfulResponseSheet() = PreviewModalBottomSheet {
    DownloadErrorSheet(
        hide = {},
        params = DownloadErrorParams(stringResource(R.string.download_error_unsuccessful_response), RichTextType.Html)
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
