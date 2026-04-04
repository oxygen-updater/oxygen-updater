package com.oxygenupdater.ui.update

import android.content.Context
import android.os.Environment
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import com.oxygenupdater.R
import com.oxygenupdater.internal.settings.KeyAdvancedMode
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.ErrorState
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.rememberState
import com.oxygenupdater.ui.main.MainRoute
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.utils.logDebug
import com.oxygenupdater.utils.logInfo
import com.oxygenupdater.workers.DirectoryRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    enqueueDownload: () -> Unit,
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

    // We're using `id` not just for efficiency, but also because of https://kotlinlang.slack.com/archives/CJLTWPH7S/p1693203706074269.
    // The `key` overload was deprecated in Compose 1.9.0-alpha02.
    val updateData = if (!refreshing) rememberSaveable(data.id) { data } else data
    val filename = updateData.filename

    val currentSetSubtitleResId by rememberUpdatedState(setSubtitleResId)
    if (updateData.id == null || !updateData.isUpdateInformationAvailable
        || (updateData.systemIsUpToDate && !getPrefBool(KeyAdvancedMode, false))
    ) {
        (if (updateData.isUpdateInformationAvailable) R.string.update_information_system_is_up_to_date
        else R.string.update_information_no_update_data_available).let {
            // Lifecycle is only STARTED when transitioning (e.g. predictive back).
            // RESUMED is reached only if this screen is fully active, which is when
            // we want the subtitle to be updated.
            LifecycleResumeEffect(it) {
                currentSetSubtitleResId(it)
                onPauseOrDispose {}
            }
        }

        MainRoute.Update.badge = null

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
            // Lifecycle is only STARTED when transitioning (e.g. predictive back).
            // RESUMED is reached only if this screen is fully active, which is when
            // we want the subtitle to be updated.
            LifecycleResumeEffect(it) {
                currentSetSubtitleResId(it)
                onPauseOrDispose {}
            }
        }

        MainRoute.Update.badge = if (updateData.systemIsUpToDate) null else "new"

        val context = LocalContext.current
        var downloadStatus by rememberState(_downloadStatus, _downloadStatus)
        LifecycleResumeEffect(context, filename) {
            // Correct download status onResume. Some logic is duplicated across branches,
            // but it's done this way so that we only launch a coroutine when we need to:
            // file ops on the IO thread to avoid main thread ANRs.
            if (filename == null) {
                logInfo(TAG, "Can't check if download completed/paused; filename = null")
                downloadStatus = correctStatusWhenFilenameIsNull(downloadStatus)
            } else lifecycleScope.launch(Dispatchers.IO) {
                downloadStatus = correctStatus(downloadStatus, filename, context)
            }

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
                    DownloadAction.Enqueue -> enqueueDownload()

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

/** To be used for basic/fallback corrections when filename is null */
private inline fun correctStatusWhenFilenameIsNull(status: DownloadStatus) = when (status) {
    // File doesn't exist, correct existing status to NotDownloading
    // (e.g. user manually deleted the file and switched back to the app)
    DownloadStatus.DownloadCompleted -> {
        logDebug(TAG, "OTA ZIP does not exist; setting status to NOT_DOWNLOADING")
        DownloadStatus.NotDownloading
    }

    DownloadStatus.NotDownloading, DownloadStatus.DownloadPaused -> {
        logDebug(TAG, "Temporary OTA ZIP does not exist; setting status to NOT_DOWNLOADING")
        DownloadStatus.NotDownloading
    }

    else -> {
        logDebug(TAG, "No download status corrections needed; $status")
        status
    }
}

/**
 * To be used when we need to perform file ops to check completed/paused status. Offloaded to an
 * I/O thread to avoid rare main thread ANRs. The exact Firebase-reported ANR log for file ops is:
 * > This binder call may be taking too long, causing the main thread to wait and triggering the ANR
 *
 * Firebase insight:
 * > The main thread was busy doing a binder call that was potentially slow. The latency of a binder call is
 * > hard to predict. It can be affected not only by the complexity of the call itself, but also by
 * > intermittent factors such as system server lock contention. You should treat them as you would treat I/O
 * > calls, and avoid, if possible, making binder calls in the main thread. If making a binder call from the
 * > main thread is unavoidable, make sure that you instrument and monitor such calls to detect slowness.
 */
@Suppress("RedundantSuspendModifier") // must be called from an IO coroutine
private suspend inline fun correctStatus(
    status: DownloadStatus,
    filename: String,
    context: Context,
) = if (
// Check if completed, i.e. final file exists
    File(Environment.getExternalStoragePublicDirectory(DirectoryRoot), filename).exists()
) {
    logDebug(TAG, "$filename exists; setting status to COMPLETED")
    DownloadStatus.DownloadCompleted
} else when (status) {
    // If previous condition was false, file doesn't exist, so make sure we correct existing status
    // (e.g. user manually deleted the file and switched back to the app)
    DownloadStatus.DownloadCompleted -> {
        logDebug(TAG, "$filename does not exist; setting status to NOT_DOWNLOADING")
        DownloadStatus.NotDownloading
    }

    // Check if paused, i.e. temporary file exists
    DownloadStatus.NotDownloading, DownloadStatus.DownloadPaused -> if (
        File(context.getExternalFilesDir(null), filename).exists()
    ) {
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
