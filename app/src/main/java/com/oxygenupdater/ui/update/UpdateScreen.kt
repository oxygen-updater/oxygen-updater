package com.oxygenupdater.ui.update

import android.content.Context
import android.os.Environment
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.oxygenupdater.R
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.ErrorState
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.main.Screen
import com.oxygenupdater.utils.logDebug
import com.oxygenupdater.utils.logInfo
import com.oxygenupdater.workers.DirectoryRoot
import java.io.File

@Composable
fun UpdateScreen(
    navType: NavType,
    windowWidthSize: WindowWidthSizeClass,
    state: RefreshAwareState<UpdateData?>,
    refresh: () -> Unit,
    @Suppress("LocalVariableName") _downloadStatus: DownloadStatus,
    failureType: Int?,
    workProgress: WorkProgress?,
    forceDownloadErrorDialog: Boolean,
    setSubtitleResId: (Int) -> Unit,
    enqueueDownload: (UpdateData) -> Unit,
    pauseDownload: () -> Unit,
    cancelDownload: (filename: String?) -> Unit,
    deleteDownload: (filename: String?) -> Boolean,
    logDownloadError: () -> Unit,
) = PullRefresh(state, { it == null }, refresh) {
    val (refreshing, data) = state
    if (data == null) {
        ErrorState(
            navType = navType,
            titleResId = R.string.update_information_error_title,
            refresh = refresh,
        )
        return@PullRefresh // skip the rest
    }

    // TODO(compose): remove `key` if https://kotlinlang.slack.com/archives/CJLTWPH7S/p1693203706074269 is resolved
    val updateData = if (!refreshing) rememberSaveable(data, key = data.id.toString()) { data } else data
    val filename = updateData.filename

    if (updateData.id == null || !updateData.isUpdateInformationAvailable
        || (updateData.systemIsUpToDate && !PrefManager.getBoolean(PrefManager.KeyAdvancedMode, false))
    ) {
        (if (updateData.isUpdateInformationAvailable) R.string.update_information_system_is_up_to_date
        else R.string.update_information_no_update_data_available).let {
            LaunchedEffect(it) { setSubtitleResId(it) }
        }

        Screen.Update.badge = null

        UpToDate(
            navType = navType,
            windowWidthSize = windowWidthSize,
            refreshing = refreshing,
            updateData = updateData,
        )
    } else {
        (if (updateData.systemIsUpToDate) R.string.update_information_header_advanced_mode_hint
        else R.string.update_notification_channel_name).let {
            LaunchedEffect(it) { setSubtitleResId(it) }
        }

        Screen.Update.badge = if (updateData.systemIsUpToDate) null else "new"

        val context = LocalContext.current
        var downloadStatus by remember(_downloadStatus) { mutableStateOf(_downloadStatus) }
        LifecycleResumeEffect(context, filename) {
            // Correct download status onResume
            downloadStatus = correctStatus(context, downloadStatus, filename)
            onPauseOrDispose {}
        }

        UpdateAvailable(
            navType = navType,
            windowWidthSize = windowWidthSize,
            refreshing = refreshing,
            updateData = updateData,
            downloadStatus = downloadStatus,
            failureType = failureType,
            workProgress = workProgress,
            forceDownloadErrorDialog = forceDownloadErrorDialog,
            downloadAction = {
                when (it) {
                    DownloadAction.Enqueue -> enqueueDownload(updateData)

                    DownloadAction.Pause -> {
                        pauseDownload()
                        downloadStatus = DownloadStatus.DownloadPaused
                    }

                    DownloadAction.Cancel -> {
                        cancelDownload(filename)
                        downloadStatus = DownloadStatus.NotDownloading
                    }

                    DownloadAction.Delete -> {
                        // Change status only if the file was successfully deleted
                        if (deleteDownload(filename)) downloadStatus = DownloadStatus.NotDownloading
                    }
                }
            },
            logDownloadError = logDownloadError,
        )
    }
}

private fun checkDownloadCompleted(filename: String?) = if (filename == null) {
    logInfo(TAG, "Can't check download completion; filename = null")
    false
} else File(Environment.getExternalStoragePublicDirectory(DirectoryRoot), filename).exists()

private fun checkDownloadPaused(context: Context?, filename: String?) = if (filename == null) {
    logInfo(TAG, "Can't check download paused; filename = null")
    false
} else context?.let {
    File(it.getExternalFilesDir(null), filename).exists()
} ?: false

private fun correctStatus(
    context: Context?,
    status: DownloadStatus,
    filename: String?,
) = if (checkDownloadCompleted(filename)) {
    logDebug(TAG, "$filename exists; setting status to COMPLETED")
    DownloadStatus.DownloadCompleted
} else when (status) {
    // If previous condition was false, file doesn't exist, so make sure we correct existing status
    // (e.g. user manually deleted the file and switched back to the app)
    DownloadStatus.DownloadCompleted -> {
        logDebug(TAG, "$filename does not exist; setting status to NOT_DOWNLOADING")
        DownloadStatus.NotDownloading
    }
    // Correct initial status by checking for pause status (e.g. user paused and exited the app, then returned later)
    DownloadStatus.NotDownloading, DownloadStatus.DownloadPaused -> if (checkDownloadPaused(context, filename)) {
        logDebug(TAG, "Temporary $filename exists; setting status to PAUSED")
        // Temp file exists; download was in progress but paused by user
        DownloadStatus.DownloadPaused
    } else {
        logDebug(TAG, "Temporary $filename does not exist; setting status to NOT_DOWNLOADING")
        DownloadStatus.NotDownloading
    }

    else -> {
        logDebug(TAG, "No download status corrections needed; $status")
        status
    }
}

private const val TAG = "UpdateScreen"

const val KeyDownloadErrorMessage = "download_error_message"
const val KeyDownloadErrorResumable = "download_error_resumable"
