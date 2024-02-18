package com.oxygenupdater.ui.dialogs

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.oxygenupdater.R
import com.oxygenupdater.ui.common.OutlinedIconButton

@Composable
@NonRestartableComposable
fun AlertDialog(
    action: (result: Boolean) -> Unit,
    @StringRes titleResId: Int,
    text: String,
    confirmIconAndResId: Pair<ImageVector, Int>? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) = AlertDialog(
    onDismissRequest = { action(false) },
    confirmButton = confirm@{
        val (icon, resId) = confirmIconAndResId ?: return@confirm
        OutlinedIconButton(
            onClick = { action(true) },
            icon = icon,
            textResId = resId,
        )
    },
    dismissButton = {
        TextButton(
            onClick = { action(false) },
            colors = textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.testTag(AlertDialog_DismissButtonTestTag)
        ) {
            Text(stringResource(R.string.download_error_close))
        }
    },
    title = {
        Text(
            text = stringResource(titleResId),
            modifier = Modifier.testTag(AlertDialog_TitleTestTag)
        )
    },
    text = {
        if (content != null) Column(Modifier.testTag(AlertDialog_ContentTestTag)) {
            Text(text, modifier = Modifier.testTag(AlertDialog_TextTestTag))
            content()
        } else Text(text, modifier = Modifier.testTag(AlertDialog_TextTestTag))
    },
    properties = NonCancellableDialog,
    modifier = Modifier.testTag(AlertDialogTestTag)
)

private const val TAG = "AlertDialog"

@VisibleForTesting
const val AlertDialogTestTag = TAG

@VisibleForTesting
const val AlertDialog_DismissButtonTestTag = TAG + "_DismissButton"

@VisibleForTesting
const val AlertDialog_TitleTestTag = TAG + "_Title"

@VisibleForTesting
const val AlertDialog_TextTestTag = TAG + "_Text"

@VisibleForTesting
const val AlertDialog_ContentTestTag = TAG + "_Content"
