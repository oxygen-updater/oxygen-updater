package com.oxygenupdater.compose.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
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
import com.oxygenupdater.compose.ui.common.OutlinedIconButton
import com.oxygenupdater.compose.ui.theme.PreviewThemes

@Composable
fun AlreadyDownloadedSheet(
    hide: () -> Unit,
    onClick: (Boolean) -> Unit,
) {
    SheetHeader(R.string.delete_message_title)

    Text(
        stringResource(R.string.delete_message_contents),
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        style = MaterialTheme.typography.bodyMedium
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton({
            onClick(false)
            hide()
        }, Modifier.padding(end = 8.dp), colors = textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text(stringResource(R.string.delete_message_delete_button))
        }

        OutlinedIconButton({
            onClick(true)
            hide()
        }, Icons.AutoMirrored.Rounded.Launch, R.string.install)
    }
}

@PreviewThemes
@Composable
fun PreviewAlreadyDownloadedSheet() = PreviewModalBottomSheet {
    AlreadyDownloadedSheet(hide = {}) {}
}

