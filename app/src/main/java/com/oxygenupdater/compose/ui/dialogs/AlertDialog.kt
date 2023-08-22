package com.oxygenupdater.compose.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.common.OutlinedIconButton

@Composable
fun AlertDialog(
    show: MutableState<Boolean>,
    @StringRes titleResId: Int,
    text: String,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    confirmIconAndResId: Pair<ImageVector, Int>? = null,
    action: ((Boolean) -> Unit)? = null,
) {
    if (!show.value) return

    AlertDialog({
        show.value = false
        action?.invoke(false)
    }, confirmButton = confirm@{
        val (icon, resId) = confirmIconAndResId ?: return@confirm
        OutlinedIconButton({
            show.value = false
            action?.invoke(true)
        }, icon, resId)
    }, dismissButton = {
        TextButton({
            show.value = false
            action?.invoke(false)
        }, colors = textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text(stringResource(R.string.download_error_close))
        }
    }, title = {
        Text(stringResource(titleResId))
    }, text = {
        if (content != null) Column {
            Text(text)
            content()
        } else Text(text)
    }, properties = NonCancellableDialog)
}
