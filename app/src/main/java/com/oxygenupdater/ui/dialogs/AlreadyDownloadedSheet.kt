package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import com.oxygenupdater.ui.common.OutlinedIconButton
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun AlreadyDownloadedSheet(
    hide: () -> Unit,
    onClick: (result: Boolean) -> Unit,
) {
    SheetHeader(R.string.delete_message_title)

    Text(
        text = stringResource(R.string.delete_message_contents),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    )

    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifierMaxWidth.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)
    ) {
        TextButton(
            onClick = {
                onClick(false)
                hide()
            },
            colors = textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(stringResource(R.string.delete_message_delete_button))
        }

        OutlinedIconButton(
            onClick = {
                onClick(true)
                hide()
            },
            icon = Icons.AutoMirrored.Rounded.Launch,
            textResId = R.string.install,
        )
    }
}

@PreviewThemes
@Composable
fun PreviewAlreadyDownloadedSheet() = PreviewModalBottomSheet {
    AlreadyDownloadedSheet(hide = {}, onClick = {})
}
