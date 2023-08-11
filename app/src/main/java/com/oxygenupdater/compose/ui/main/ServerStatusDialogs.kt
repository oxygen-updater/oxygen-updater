package com.oxygenupdater.compose.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.oxygenupdater.R
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.PlayStore
import com.oxygenupdater.compose.ui.dialogs.AlertDialog
import com.oxygenupdater.models.ServerStatus

@Composable
fun ServerStatusDialogs(
    status: ServerStatus.Status?,
    openPlayStorePage: () -> Unit,
) {
    if (status?.isNonRecoverableError != true) return

    if (status == ServerStatus.Status.MAINTENANCE) AlertDialog(
        remember { mutableStateOf(true) },
        titleResId = R.string.error_maintenance,
        text = stringResource(R.string.error_maintenance_message),
    ) else if (status == ServerStatus.Status.OUTDATED) AlertDialog(
        remember { mutableStateOf(true) },
        titleResId = R.string.error_app_outdated,
        text = stringResource(R.string.error_app_outdated_message),
        confirmIconAndResId = CustomIcons.PlayStore to R.string.error_google_play_button_text
    ) {
        if (it) openPlayStorePage()
    }
}
