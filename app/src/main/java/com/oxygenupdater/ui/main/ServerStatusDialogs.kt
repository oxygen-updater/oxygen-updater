package com.oxygenupdater.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.PlayStore
import com.oxygenupdater.models.ServerStatus.Status
import com.oxygenupdater.ui.dialogs.AlertDialog

@Composable
fun ServerStatusDialogs(
    status: Status?,
    openPlayStorePage: () -> Unit,
) {
    if (status?.isNonRecoverableError != true) return

    val titleResId: Int
    val textResId: Int
    val confirmIconAndResId: Pair<ImageVector, Int>?
    val action: ((result: Boolean) -> Unit)?
    when (status) {
        Status.MAINTENANCE -> {
            titleResId = R.string.error_maintenance
            textResId = R.string.error_maintenance_message
            confirmIconAndResId = null
            action = null
        }

        Status.OUTDATED -> {
            titleResId = R.string.error_app_outdated
            textResId = R.string.error_app_outdated_message
            confirmIconAndResId = CustomIcons.PlayStore to R.string.error_google_play_button_text
            action = { if (it) openPlayStorePage() }
        }

        else -> return
    }

    var show by remember { mutableStateOf(true) }
    if (show) AlertDialog(
        action = {
            action?.invoke(it)
            show = false
        },
        titleResId = titleResId,
        text = stringResource(textResId),
        confirmIconAndResId = confirmIconAndResId,
    )
}
