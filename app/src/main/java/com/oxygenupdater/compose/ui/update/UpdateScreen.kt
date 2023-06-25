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
import androidx.work.WorkInfo
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.PullRefresh
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.compose.ui.common.ErrorState
import com.oxygenupdater.compose.ui.main.Screen
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.utils.Logger.logInfo
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.workers.DIRECTORY_ROOT
import java.io.File
import java.time.LocalDateTime

@Composable
fun UpdateScreen(
    state: RefreshAwareState<UpdateData?>,
    workInfoWithStatus: Pair<WorkInfo?, DownloadStatus>,
    forceDownloadErrorDialog: Boolean,
    refresh: () -> Unit,
    setSubtitle: (String) -> Unit,
    enqueueDownload: (UpdateData) -> Unit,
    pauseDownload: () -> Unit,
    cancelDownload: (String?) -> Unit,
    deleteDownload: (String?) -> Boolean,
    logDownloadError: (Data) -> Unit,
) = PullRefresh(state, onRefresh = refresh) {
    val (refreshing, data) = state
    val updateData = if (!refreshing && data != null) rememberSaveable { data } else data
    if (updateData == null) {
        ErrorState(stringResource(R.string.update_information_error_title), refresh)
        return@PullRefresh // skip the rest
    }

    val filename = updateData.filename
    // Save update data for offline viewing
    if (Utils.checkNetworkConnection()) PrefManager.apply {
        putLong(PROPERTY_OFFLINE_ID, updateData.id ?: -1L)
        putString(PROPERTY_OFFLINE_UPDATE_NAME, updateData.versionNumber)
        putLong(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, updateData.downloadSize)
        putString(PROPERTY_OFFLINE_UPDATE_DESCRIPTION, updateData.description)
        putString(PROPERTY_OFFLINE_FILE_NAME, filename)
        putString(PROPERTY_OFFLINE_DOWNLOAD_URL, updateData.downloadUrl)
        putBoolean(PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, updateData.isUpdateInformationAvailable)
        putString(PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now(Utils.SERVER_TIME_ZONE).toString())
        putBoolean(PROPERTY_OFFLINE_IS_UP_TO_DATE, updateData.systemIsUpToDate)
    }

    if (updateData.id == null || !updateData.isUpdateInformationAvailable
        || (updateData.systemIsUpToDate && !PrefManager.advancedMode.value)
    ) {
        val subtitle = stringResource(
            if (updateData.isUpdateInformationAvailable) R.string.update_information_system_is_up_to_date
            else R.string.update_information_no_update_data_available
        )
        LaunchedEffect(Unit) { setSubtitle(subtitle) }

        Screen.Update.badge = null

        UpToDate(refreshing, updateData)
    } else {
        val subtitle = stringResource(
            if (updateData.systemIsUpToDate) R.string.update_information_header_advanced_mode_hint
            else R.string.update_notification_channel_name
        )
        LaunchedEffect(Unit) { setSubtitle(subtitle) }

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

        UpdateAvailable(refreshing, updateData, workInfo, downloadStatus, forceDownloadErrorDialog, downloadAction = {
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
