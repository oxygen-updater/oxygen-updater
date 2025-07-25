package com.oxygenupdater.ui.update

import android.content.Context
import android.os.Environment
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.oxygenupdater.R
import com.oxygenupdater.internal.settings.KeyAdvancedMode
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.ErrorState
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.rememberState
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
    onRefresh: () -> Unit,
    @Suppress("LocalVariableName") _downloadStatus: DownloadStatus,
    failureType: Int,
    httpFailureCodeAndMessage: () -> Pair<Int, String>?,
    workProgress: WorkProgress?,
    forceDownloadErrorDialog: Boolean,
    getPrefStr: (key: String, default: String) -> String,
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    setSubtitleResId: (Int) -> Unit,
    enqueueDownload: (UpdateData) -> Unit,
    pauseDownload: () -> Unit,
    cancelDownload: (filename: String?) -> Unit,
    deleteDownload: (filename: String?) -> Boolean,
    openInstallGuide: () -> Unit,
    logDownloadError: () -> Unit,
    hideDownloadCompleteNotification: () -> Unit,
    showDownloadFailedNotification: () -> Unit,
) = PullRefresh(state, { it == null }, onRefresh) {
    val (refreshing, data) = state
    if (data == null) {
        ErrorState(
            navType = navType,
            titleResId = R.string.update_information_error_title,
            onRefreshClick = onRefresh,
        )
        return@PullRefresh // skip the rest
    }

    // TODO(compose): remove `key` if https://kotlinlang.slack.com/archives/CJLTWPH7S/p1693203706074269 is resolved
    val updateData = if (!refreshing) rememberSaveable(data, key = data.id.toString()) { data } else data
    val filename = updateData.filename

    if (updateData.id == null || !updateData.isUpdateInformationAvailable
        || (updateData.systemIsUpToDate && !getPrefBool(KeyAdvancedMode, false))
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
            getPrefStr = getPrefStr,
        )
    } else {
        (if (updateData.systemIsUpToDate) R.string.update_information_header_advanced_mode_hint
        else R.string.update_notification_channel_name).let {
            LaunchedEffect(it) { setSubtitleResId(it) }
        }

        Screen.Update.badge = if (updateData.systemIsUpToDate) null else "new"

        val context = LocalContext.current
        var downloadStatus by rememberState(_downloadStatus, _downloadStatus)
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
            httpFailureCodeAndMessage = httpFailureCodeAndMessage,
            workProgress = workProgress,
            forceDownloadErrorDialog = forceDownloadErrorDialog,
            getPrefStr = getPrefStr,
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
            openInstallGuide = openInstallGuide,
            logDownloadError = logDownloadError,
            hideDownloadCompleteNotification = hideDownloadCompleteNotification,
            showDownloadFailedNotification = showDownloadFailedNotification,
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
} == true

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

private const val PreviewChangelogPrefix = """#KB2001_13.1.0.513(EX01)
##2023-06-10

A system update is available. The OxygenOS 13.1 update brings new Zen Space features, a new TalkBack feature to describe images, and better gaming performance and experience.

"""

private const val PreviewChangelog = """##Personalization
• Expands Omoji's functionality and library.

##Health
• Adds a new TalkBack feature that recognizes and announces images in apps and Photos.
• Adds the new Zen Space app, with two modes, Deep Zen and Light Zen, to help you focus on the present.
• Improves Simple mode with a new helper widget and quick tutorials on the Home screen.

##Gaming experience
• Adds the Championship mode to Game Assistant. This mode improves performance while also disabling notifications, calls, and other messages to give you a more immersive gaming experience.
• Adds a music playback control to Game Assistant, so you can listen to and control music easily while gaming."""

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
val PreviewUpdateData = UpdateData(
    id = 1,
    versionNumber = "KB2001_11_F.66",
    otaVersionNumber = "KB2001_11.F.66_2660_202305041648",
    changelog = PreviewChangelog,
    description = PreviewChangelogPrefix + PreviewChangelog,
    downloadUrl = "https://gauss-componentotacostmanual-in.allawnofs.com/remove-a7779e2dc9b4b40458be6db38b226089/component-ota/23/03/15/4b70c7244ce7411994c97313e8ceb82d.zip",
    downloadSize = 4777312256,
    filename = "4b70c7244ce7411994c97313e8ceb82d.zip",
    md5sum = "0dc48e34ca895ae5653a32ef4daf2933",
    updateInformationAvailable = true,
    systemIsUpToDate = true,
)
