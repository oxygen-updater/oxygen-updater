package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.OpenInNew
import com.oxygenupdater.icons.Symbols
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
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .testTag(BottomSheet_ContentTestTag)
    )

    SheetButtons(
        dismissResId = R.string.delete_message_delete_button,
        onDismiss = { onClick(false); hide() },
        confirmIcon = Symbols.OpenInNew,
        confirmResId = R.string.install,
        onConfirm = { onClick(true); hide() },
    )
}

@PreviewThemes
@Composable
fun PreviewAlreadyDownloadedSheet() = PreviewModalBottomSheet {
    AlreadyDownloadedSheet(hide = {}, onClick = {})
}
