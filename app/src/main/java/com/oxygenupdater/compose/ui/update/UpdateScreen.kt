package com.oxygenupdater.compose.ui.update

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.Data
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.compose.ui.common.ErrorState
import com.oxygenupdater.compose.ui.common.PullRefresh
import com.oxygenupdater.compose.ui.common.rememberTypedCallback
import com.oxygenupdater.compose.ui.main.Screen
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.utils.Logger.logInfo
import com.oxygenupdater.workers.DIRECTORY_ROOT
import java.io.File

@Composable
fun UpdateScreen(
    state: RefreshAwareState<UpdateData?>,
    workInfoWithStatus: WorkInfoWithStatus,
    forceDownloadErrorDialog: Boolean,
    refresh: () -> Unit,
    setSubtitleResId: (Int) -> Unit,
    enqueueDownload: (UpdateData) -> Unit,
    pauseDownload: () -> Unit,
    cancelDownload: (String?) -> Unit,
    deleteDownload: (String?) -> Boolean,
    logDownloadError: (Data) -> Unit,
) = PullRefresh(state, { it == null }, refresh) {
    val (refreshing, data) = state
    if (data == null) {
        ErrorState(stringResource(R.string.update_information_error_title), refresh)
        return@PullRefresh // skip the rest
    }

    // TODO(compose): remove `key` if https://kotlinlang.slack.com/archives/CJLTWPH7S/p1693203706074269 is resolved
    val updateData = if (!refreshing) rememberSaveable(data, key = data.id.toString()) { data } else data
    val filename = updateData.filename

    if (updateData.id == null || !updateData.isUpdateInformationAvailable
        || (updateData.systemIsUpToDate && !PrefManager.getBoolean(PrefManager.PROPERTY_ADVANCED_MODE, false))
    ) {
        (if (updateData.isUpdateInformationAvailable) R.string.update_information_system_is_up_to_date
        else R.string.update_information_no_update_data_available).let {
            LaunchedEffect(it) { setSubtitleResId(it) }
        }

        Screen.Update.badge = null

        UpToDate(refreshing, updateData)
    } else {
        (if (updateData.systemIsUpToDate) R.string.update_information_header_advanced_mode_hint
        else R.string.update_notification_channel_name).let {
            LaunchedEffect(it) { setSubtitleResId(it) }
        }

        Screen.Update.badge = if (updateData.systemIsUpToDate) null else "new"

        val context = LocalContext.current
        val (workInfo, _downloadStatus) = workInfoWithStatus
        var downloadStatus by remember(_downloadStatus, filename) {
            mutableStateOf(correctStatus(context, _downloadStatus, filename))
        }

        // Re-run above onResume
        val observer = remember(downloadStatus, filename) {
            LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver

                downloadStatus = correctStatus(context, downloadStatus, filename)
            }
        }

        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, observer) {
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }

        UpdateAvailable(refreshing, updateData, workInfo, downloadStatus, forceDownloadErrorDialog, downloadAction = rememberTypedCallback(updateData) {
            when (it) {
                DownloadAction.Enqueue -> enqueueDownload(updateData)

                DownloadAction.Pause -> {
                    pauseDownload()
                    downloadStatus = DownloadStatus.DOWNLOAD_PAUSED
                }

                DownloadAction.Cancel -> {
                    cancelDownload(filename)
                    downloadStatus = DownloadStatus.NOT_DOWNLOADING
                }

                DownloadAction.Delete -> {
                    // Change status only if the file was successfully deleted
                    if (deleteDownload(filename)) downloadStatus = DownloadStatus.NOT_DOWNLOADING
                }
            }
        }, logDownloadError)
    }
}

private fun checkDownloadCompleted(filename: String?) = if (filename == null) {
    logInfo(TAG, "Can't check download completion; filename = null")
    false
} else File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT), filename).exists()

private fun checkDownloadPaused(context: Context?, filename: String?) = if (filename == null) {
    logInfo(TAG, "Can't check download paused; filename = null")
    false
} else context?.let {
    File(it.getExternalFilesDir(null), filename).exists()
} ?: false

fun correctStatus(context: Context?, status: DownloadStatus, filename: String?) = when {
    // File already existing implies download is complete
    checkDownloadCompleted(filename) -> DownloadStatus.DOWNLOAD_COMPLETED
    status == DownloadStatus.NOT_DOWNLOADING -> if (checkDownloadPaused(context, filename)) {
        // Temp file existing implies download was in progress, so user must have paused it
        DownloadStatus.DOWNLOAD_PAUSED
    } else status

    else -> status
}

private const val TAG = "UpdateScreen"

const val KEY_DOWNLOAD_ERROR_MESSAGE = "download_error_message"
const val KEY_DOWNLOAD_ERROR_RESUMABLE = "download_error_resumable"
