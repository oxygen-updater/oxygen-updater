package com.oxygenupdater.ui.update

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.storage.StorageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import androidx.work.Data
import androidx.work.WorkInfo
import com.oxygenupdater.R
import com.oxygenupdater.extensions.formatFileSize
import com.oxygenupdater.extensions.startInstallActivity
import com.oxygenupdater.ui.common.RichTextType
import com.oxygenupdater.ui.common.rememberCallback
import com.oxygenupdater.ui.onboarding.NOT_SET
import com.oxygenupdater.ui.onboarding.NOT_SET_L
import com.oxygenupdater.ui.update.DownloadStatus.DOWNLOADING
import com.oxygenupdater.ui.update.DownloadStatus.DOWNLOAD_COMPLETED
import com.oxygenupdater.ui.update.DownloadStatus.DOWNLOAD_FAILED
import com.oxygenupdater.ui.update.DownloadStatus.DOWNLOAD_PAUSED
import com.oxygenupdater.ui.update.DownloadStatus.DOWNLOAD_QUEUED
import com.oxygenupdater.ui.update.DownloadStatus.NOT_DOWNLOADING
import com.oxygenupdater.ui.update.DownloadStatus.VERIFICATION_COMPLETED
import com.oxygenupdater.ui.update.DownloadStatus.VERIFICATION_FAILED
import com.oxygenupdater.ui.update.DownloadStatus.VERIFYING
import com.oxygenupdater.utils.LocalNotifications
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_BYTES_DONE
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_ETA
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_FAILURE_TYPE
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_PROGRESS
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_TOTAL_BYTES
import java.io.IOException
import java.util.UUID

@Immutable
data class DownloadButtonConfig(
    val titleResId: Int,
    val details: String?,
    val sizeOrProgressText: String,
    val progress: Float?, // null => hidden, 0 => indeterminate
    val actionButtonConfig: Triple<Boolean, ImageVector, Color>?,
    val onDownloadClick: (() -> Unit)?,
)

@Composable
fun downloadButtonConfig(
    downloadSize: Long,
    workInfo: WorkInfo?,
    downloadStatus: DownloadStatus,
    downloadAction: (DownloadAction) -> Unit,
    logDownloadError: (Data) -> Unit,
    hasDownloadPermissions: () -> Boolean,
    requestDownloadPermissions: () -> Unit,
    showAlreadyDownloadedSheet: () -> Unit,
    setCanShowDownloadErrorDialog: () -> Unit,
    setDownloadErrorDialogParams: (DownloadErrorParams) -> Unit,
    setManageStorageDialogData: (Pair<UUID, Long>) -> Unit,
): DownloadButtonConfig {
    val context = LocalContext.current

    val enqueueIfSpaceAvailable = rememberCallback(context, downloadSize) {
        enqueueIfSpaceAvailable(
            context, downloadSize,
            setCanShowDownloadErrorDialog, setDownloadErrorDialogParams, setManageStorageDialogData
        ) { downloadAction(DownloadAction.Enqueue) }
    }

    return when (downloadStatus) {
        NOT_DOWNLOADING -> {
            previousProgress = null

            val outputData = workInfo?.outputData
            val failureType = outputData?.getInt(WORK_DATA_DOWNLOAD_FAILURE_TYPE, NOT_SET)
            if (failureType != null && failureType != NOT_SET) when (failureType) {
                DownloadFailure.ServerError.value,
                DownloadFailure.ConnectionError.value,
                -> setDownloadErrorDialogParams(DownloadErrorParams(
                    stringResource(R.string.download_error_server),
                    resumable = failureType == DownloadFailure.ConnectionError.value
                ) { resumable ->
                    if (!resumable) downloadAction(DownloadAction.Delete)
                    enqueueIfSpaceAvailable()
                })

                DownloadFailure.UnsuccessfulResponse.value -> setDownloadErrorDialogParams(
                    DownloadErrorParams(
                        stringResource(R.string.download_error_unsuccessful_response), RichTextType.Html
                    )
                ).also { logDownloadError(outputData) }

                DownloadFailure.NullUpdateDataOrDownloadUrl.value,
                DownloadFailure.DownloadUrlInvalidScheme.value,
                DownloadFailure.Unknown.value,
                -> setDownloadErrorDialogParams(DownloadErrorParams(
                    stringResource(R.string.download_error_internal)
                ) { resumable ->
                    if (!resumable) downloadAction(DownloadAction.Delete)
                    enqueueIfSpaceAvailable()
                })

                DownloadFailure.CouldNotMoveTempFile.value -> setDownloadErrorDialogParams(
                    DownloadErrorParams(
                        stringResource(
                            R.string.download_error_could_not_move_temp_file,
                            "Android/data/${context.packageName}/files"
                        )
                    )
                )

                else -> {}
            }

            DownloadButtonConfig(
                titleResId = R.string.download,
                details = null,
                sizeOrProgressText = context.formatFileSize(downloadSize),
                progress = null,
                actionButtonConfig = null,
                onDownloadClick = if (hasDownloadPermissions()) enqueueIfSpaceAvailable else requestDownloadPermissions,
            )
        }

        DOWNLOAD_FAILED -> DownloadButtonConfig(
            titleResId = R.string.download,
            details = stringResource(R.string.download_failed),
            sizeOrProgressText = context.formatFileSize(downloadSize),
            progress = null,
            actionButtonConfig = null,
            onDownloadClick = null,
        ).also { previousProgress = null }

        DOWNLOAD_QUEUED, DOWNLOADING -> {
            val bytesDone: Long
            val totalBytes: Long
            val currentProgress: Int
            val workProgress = workInfo?.progress
            val details = if (workProgress != null) workProgress.run {
                bytesDone = getLong(WORK_DATA_DOWNLOAD_BYTES_DONE, NOT_SET_L)
                totalBytes = getLong(WORK_DATA_DOWNLOAD_TOTAL_BYTES, NOT_SET_L)
                currentProgress = getInt(WORK_DATA_DOWNLOAD_PROGRESS, 0)
                getString(WORK_DATA_DOWNLOAD_ETA) ?: stringResource(R.string.summary_please_wait)
            } else {
                bytesDone = NOT_SET_L
                totalBytes = NOT_SET_L
                currentProgress = 0
                stringResource(R.string.summary_please_wait)
            }

            val sizeOrProgressText = if (bytesDone != NOT_SET_L && totalBytes != NOT_SET_L) {
                val bytesDoneStr = context.formatFileSize(bytesDone)
                val totalBytesStr = context.formatFileSize(totalBytes)
                "$bytesDoneStr / $totalBytesStr ($currentProgress%)"
            } else previousProgressText ?: context.formatFileSize(downloadSize)
            previousProgressText = sizeOrProgressText

            val progress = if (currentProgress == 0) 0f else currentProgress / 100f
            previousProgress = progress

            DownloadButtonConfig(
                titleResId = R.string.downloading,
                details = details,
                sizeOrProgressText = sizeOrProgressText,
                progress = progress,
                actionButtonConfig = Triple(true, Icons.Rounded.Close, MaterialTheme.colorScheme.error),
                onDownloadClick = { downloadAction(DownloadAction.Pause) },
            )
        }

        DOWNLOAD_PAUSED -> DownloadButtonConfig(
            titleResId = R.string.paused,
            details = stringResource(R.string.download_progress_text_paused),
            sizeOrProgressText = previousProgressText ?: context.formatFileSize(downloadSize),
            progress = previousProgress,
            actionButtonConfig = Triple(true, Icons.Rounded.Close, MaterialTheme.colorScheme.error),
            onDownloadClick = if (hasDownloadPermissions()) enqueueIfSpaceAvailable else requestDownloadPermissions,
        )

        DOWNLOAD_COMPLETED, VERIFICATION_COMPLETED -> DownloadButtonConfig(
            titleResId = R.string.downloaded,
            details = null,
            sizeOrProgressText = context.formatFileSize(downloadSize),
            progress = null,
            actionButtonConfig = Triple(false, Icons.AutoMirrored.Rounded.Launch, MaterialTheme.colorScheme.primary),
            onDownloadClick = showAlreadyDownloadedSheet,
        ).also {
            // Open install guide automatically, but only after the normal download flow completes
            LaunchedEffect(Unit) {
                if (previousProgress != null) context.startInstallActivity(false)
                previousProgress = null // reset
            }
        }

        VERIFYING -> DownloadButtonConfig(
            titleResId = R.string.download_verifying,
            details = stringResource(R.string.download_progress_text_verifying),
            sizeOrProgressText = context.formatFileSize(downloadSize),
            progress = 0f,
            actionButtonConfig = null,
            onDownloadClick = null,
        )

        VERIFICATION_FAILED -> DownloadButtonConfig(
            titleResId = R.string.download_verifying_error,
            details = stringResource(R.string.download_notification_error_corrupt),
            sizeOrProgressText = context.formatFileSize(downloadSize),
            progress = null,
            actionButtonConfig = null,
            onDownloadClick = if (hasDownloadPermissions()) enqueueIfSpaceAvailable else requestDownloadPermissions,
        ).also { config ->
            previousProgress = null
            setDownloadErrorDialogParams(DownloadErrorParams(context.getString(R.string.download_error_corrupt)) {
                setCanShowDownloadErrorDialog()
                config.onDownloadClick?.invoke()
            })
        }
    }
}

private fun enqueueIfSpaceAvailable(
    context: Context,
    downloadSize: Long,
    setCanShowDownloadErrorDialog: () -> Unit,
    setDownloadErrorDialogParams: (DownloadErrorParams) -> Unit,
    setManageStorageDialogData: (Pair<UUID, Long>) -> Unit,
    enqueue: () -> Unit,
) {
    if (SDK_INT >= VERSION_CODES.O) {
        val externalFilesDir = context.getExternalFilesDir(null) ?: return
        val storageManager = context.getSystemService<StorageManager>() ?: return
        val appSpecificExternalDirUuid = storageManager.getUuidForPath(externalFilesDir)
        setCanShowDownloadErrorDialog()

        // Get max bytes that can be allocated by the system to the app
        // This value is usually larger than [File.usableSpace], because deletable cached files are also considered
        val allocatableBytes = storageManager.getAllocatableBytes(appSpecificExternalDirUuid) - SafeMargin
        if (downloadSize <= allocatableBytes) try {
            // Allocate bytes: the system will delete cached files if necessary to fulfil this request,
            // or throw an IOException if it fails for whatever reason.
            storageManager.allocateBytes(appSpecificExternalDirUuid, downloadSize)

            // Enqueue download work since the required space has been freed up
            enqueue()
        } catch (e: IOException) {
            // Request the user to free up space manually because the system couldn't do it automatically
            setManageStorageDialogData(appSpecificExternalDirUuid to downloadSize + SafeMargin)
        } else setManageStorageDialogData(appSpecificExternalDirUuid to downloadSize - allocatableBytes)
    } else {
        val externalFilesDir = context.getExternalFilesDir(null) ?: return
        setCanShowDownloadErrorDialog()

        // Check if there is enough free storage space before downloading
        val usableBytes = externalFilesDir.usableSpace - SafeMargin
        if (downloadSize <= usableBytes) enqueue()
        else {
            // Don't have enough space to complete the download. Display a notification and an error dialog to the user.
            LocalNotifications.showDownloadFailedNotification(
                context,
                false,
                R.string.download_error_storage,
                R.string.download_notification_error_storage_full
            )

            setDownloadErrorDialogParams(DownloadErrorParams(context.getString(R.string.download_error_storage)))
        }
    }
}

/** Amount of free storage space to reserve when downloading an update */
private const val SafeMargin = 1048576 * 25L // 25 MB

/** Used for maintaining progress text across DOWNLOADING -> PAUSED */
private var previousProgressText: String? = null
private var previousProgress: Float? = null
