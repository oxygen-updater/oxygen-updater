package com.oxygenupdater.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.storage.StorageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import com.oxygenupdater.ui.update.DownloadAction
import com.oxygenupdater.utils.logInfo
import java.util.UUID

/**
 * Shows a dialog to the user asking them to free up some space, so that the app can download the OTA ZIP successfully.
 * If the user confirms this request, [StorageManager.ACTION_MANAGE_STORAGE] is launched which shows a system-supplied
 * "Remove items" UI (API 26+) that guides the user on clearing up the required storage space.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ManageStorageSheet(
    hide: () -> Unit,
    pair: Pair<UUID, Long>?,
    downloadAction: (DownloadAction) -> Unit,
    onCancel: () -> Unit,
) {
    SheetHeader(R.string.download_notification_error_storage_full)

    Text(
        text = stringResource(R.string.download_error_storage),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    )

    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifierMaxWidth.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)
    ) {
        TextButton(
            onClick = hide,
            colors = textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(stringResource(R.string.download_error_close))
        }

        val (uuid, bytes) = pair ?: return@Row
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                // Enqueue if required space has been freed up
                Activity.RESULT_OK -> downloadAction(DownloadAction.Enqueue).also {
                    logInfo(TAG, "User freed-up space")
                }

                Activity.RESULT_CANCELED -> onCancel().also {
                    logInfo(TAG, "User didn't free-up space")
                }

                else -> logInfo(TAG, "Unhandled resultCode: ${it.resultCode}")
            }
        }
        OutlinedIconButton(
            onClick = {
                val intent = Intent(StorageManager.ACTION_MANAGE_STORAGE)
                    .putExtra(StorageManager.EXTRA_UUID, uuid)
                    .putExtra(StorageManager.EXTRA_REQUESTED_BYTES, bytes)
                launcher.launch(intent)
                hide()
            },
            icon = Icons.AutoMirrored.Rounded.Launch,
            textResId = android.R.string.ok,
        )
    }
}

private const val TAG = "ManageStorageSheet"

@RequiresApi(Build.VERSION_CODES.O)
@PreviewThemes
@Composable
fun PreviewManageStorageSheet() = PreviewModalBottomSheet {
    ManageStorageSheet(
        hide = {},
        pair = UUID.randomUUID() to 1L,
        downloadAction = {},
        onCancel = {},
    )
}
