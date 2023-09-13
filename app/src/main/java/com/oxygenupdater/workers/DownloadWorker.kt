package com.oxygenupdater.workers

import android.app.Notification
import android.app.Notification.CATEGORY_PROGRESS
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.oxygenupdater.R
import com.oxygenupdater.apis.DownloadApi
import com.oxygenupdater.extensions.formatFileSize
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.KeyDownloadBytesDone
import com.oxygenupdater.models.TimeRemaining
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.update.DownloadFailure
import com.oxygenupdater.utils.ExceptionUtils
import com.oxygenupdater.utils.LocalNotifications.showDownloadFailedNotification
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logInfo
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.DownloadStatusNotifChannelId
import com.oxygenupdater.utils.NotificationIds
import com.oxygenupdater.utils.UpdateDataVersionFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.getKoin
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Handles downloading ZIPs from OnePlus OTA servers
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class DownloadWorker(
    private val context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    private var isFirstPublish = true
    private var previousProgressTimestamp = 0L
    private val measurements = ArrayList<Double>()
    private val updateData = UpdateData.createFromWorkData(parameters.inputData)

    private lateinit var tempFile: File
    private lateinit var zipFile: File
    private lateinit var notification: Notification

    private val downloadApi: DownloadApi
    private val workManager: WorkManager
    private val notificationManager: NotificationManagerCompat

    init {
        val koin = getKoin()

        downloadApi = koin.inject<DownloadApi>().value
        workManager = koin.inject<WorkManager>().value
        notificationManager = koin.inject<NotificationManagerCompat>().value
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Mark the Worker as important
        trySetForeground(createInitialNotification())

        when {
            updateData?.downloadUrl == null -> {
                showDownloadFailedNotification(context, false, R.string.download_error_internal, R.string.download_notification_error_internal)

                Result.failure(Data.Builder().apply {
                    putInt(WorkDataDownloadFailureType, DownloadFailure.NullUpdateDataOrDownloadUrl.value)
                }.build())
            }

            !updateData.downloadUrl.contains("http") -> {
                showDownloadFailedNotification(context, false, R.string.download_error_internal, R.string.download_notification_error_internal)

                Result.failure(Data.Builder().apply {
                    putInt(WorkDataDownloadFailureType, DownloadFailure.DownloadUrlInvalidScheme.value)
                }.build())
            }

            else -> download()
        }
    }

    private fun createInitialNotification(): Notification {
        // This PendingIntent can be used to cancel the worker
        val cancelPendingIntent = workManager.createCancelPendingIntent(id)

        val text = context.getString(R.string.download_pending)

        notification = NotificationCompat.Builder(context, DownloadStatusNotifChannelId)
            .setSmallIcon(R.drawable.logo_notification)
            .setContentTitle(UpdateDataVersionFormatter.getFormattedVersionNumber(updateData))
            .setContentText(text)
            .setProgress(100, 50, true)
            .setOngoing(true)
            .setCategory(CATEGORY_PROGRESS)
            .setPriority(PRIORITY_LOW)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(
                android.R.drawable.ic_delete,
                context.getString(android.R.string.cancel),
                cancelPendingIntent
            )
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .build()

        return notification
    }

    private fun createProgressNotification(
        bytesDone: Long,
        totalBytes: Long,
        progress: Int,
        downloadEta: String?,
    ): Notification {
        // This PendingIntent can be used to cancel the worker
        val cancelPendingIntent = workManager.createCancelPendingIntent(id)

        val bytesDoneStr = context.formatFileSize(bytesDone)
        val totalBytesStr = context.formatFileSize(totalBytes)

        val text = "$bytesDoneStr / $totalBytesStr ($progress%)"

        notification = NotificationCompat.Builder(context, DownloadStatusNotifChannelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(UpdateDataVersionFormatter.getFormattedVersionNumber(updateData))
            .setContentText(text)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setCategory(CATEGORY_PROGRESS)
            .setPriority(PRIORITY_LOW)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(
                android.R.drawable.ic_delete,
                context.getString(android.R.string.cancel),
                cancelPendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle().setSummaryText(downloadEta ?: ""))
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .build()

        notificationManager.apply {
            cancel(NotificationIds.LocalDownload)
            cancel(NotificationIds.LocalMd5Verification)
        }

        return notification
    }

    /**
     * Wrapped in a try/catch to ignore a potential [java.lang.IllegalStateException] when using [WorkRequest.Builder.setExpedited].
     *
     * Android 12+ will throw a more specific [android.app.ForegroundServiceStartNotAllowedException].
     *
     * @see <a href="https://developer.android.com/guide/background/persistent/getting-started/define-work#coroutineworker">Executing expedited work — Coroutine Worker</a>
     */
    private suspend fun trySetForeground(notification: Notification) = try {
        setForeground(foregroundInfo(notification))
    } catch (e: Exception) {
        logError(TAG, "setForeground failed", e)
    }

    /** Must be overridden because we use [WorkRequest.Builder.setExpedited] */
    override suspend fun getForegroundInfo() = foregroundInfo(
        if (::notification.isInitialized) notification else createInitialNotification()
    )

    private fun foregroundInfo(notification: Notification) = if (SDK_INT >= Build.VERSION_CODES.Q) ForegroundInfo(
        NotificationIds.LocalDownloadForeground, notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC // keep in sync with `foregroundServiceType` in AndroidManifest
    ) else ForegroundInfo(NotificationIds.LocalDownloadForeground, notification)

    private suspend fun download(): Result = withContext(Dispatchers.IO) {
        tempFile = File(context.getExternalFilesDir(null), updateData!!.filename!!)
        zipFile = File(Environment.getExternalStoragePublicDirectory(DirectoryRoot).absolutePath, updateData.filename!!)

        var startingByte = PrefManager.getLong(KeyDownloadBytesDone, NotSetL)
        var rangeHeader = if (startingByte != NotSetL) "bytes=$startingByte-" else null

        if (startingByte != NotSetL) {
            logDebug(TAG, "Looks like a resume operation. Adding $rangeHeader to the request")

            if (tempFile.length() > startingByte) logWarning(
                TAG,
                "Partially downloaded ZIP size differs from skipped bytes (${tempFile.length()} vs $startingByte)."
            ) else if (tempFile.length() < startingByte) {
                logWarning(
                    TAG,
                    "Partially downloaded ZIP size (${tempFile.length()}) is lesser than skipped bytes ($startingByte). Resetting state."
                )

                startingByte = NotSetL
                rangeHeader = null
            }
        }

        val response = downloadApi.downloadZip(updateData.downloadUrl!!, rangeHeader)

        val body = response.body()

        if (!response.isSuccessful || body == null) {
            return@withContext Result.failure(Data.Builder().apply {
                putInt(WorkDataDownloadFailureType, DownloadFailure.UnsuccessfulResponse.value)
                putString(WorkDataDownloadFailureExtraUrl, updateData.downloadUrl)
                putString(WorkDataDownloadFailureExtraFilename, updateData.filename)
                putString(WorkDataDownloadFailureExtraVersion, updateData.versionNumber)
                putString(WorkDataDownloadFailureExtraOtaVersion, updateData.otaVersionNumber)
                putInt(WorkDataDownloadFailureExtraHttpCode, response.code())
                putString(WorkDataDownloadFailureExtraHttpMessage, response.message())
            }.build())
        }

        body.byteStream().use { stream ->
            logInfo(TAG, "Downloading ZIP from ${startingByte.coerceAtLeast(0L)} bytes")

            // Copy stream to file
            // We could have used the [InputStream.copyTo] extension defined in [IOStreams.kt],
            // but we need to support pause/resume functionality, as well as publish progress
            RandomAccessFile(tempFile, "rw").use { randomAccessFile ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes: Int
                var bytesRead = startingByte.coerceAtLeast(0L)
                val contentLength = bytesRead + body.contentLength()

                if (abs(contentLength - updateData.downloadSize) > ThresholdBytesDifferenceWithBackend) logWarning(
                    TAG,
                    "Content length reported by the download server differs from UpdateData.downloadSize ($contentLength vs ${updateData.downloadSize})"
                )

                if (startingByte != NotSetL) {
                    logDebug(TAG, "Looks like a resume operation. Seeking to $startingByte bytes")
                    randomAccessFile.seek(startingByte)
                }

                try {
                    while (stream.read(buffer).also { bytes = it } >= 0) {
                        randomAccessFile.write(buffer, 0, bytes)
                        bytesRead += bytes

                        publishProgressIfNeeded(bytesRead, contentLength)
                    }
                } catch (e: Exception) {
                    return@withContext if (isStopped) {
                        logDebug(TAG, "Ignoring exception since worker is in stopped state: ${e.message}")
                        Result.success()
                    } else {
                        logError(TAG, "Exception while reading from byteStream", e)

                        val retryCount = runAttemptCount + 1

                        when {
                            ExceptionUtils.isNetworkError(e) -> {
                                if (retryCount <= 5) {
                                    logDebug(TAG, "Network error encountered. Retrying ($retryCount/5)")
                                    Result.retry()
                                } else {
                                    showDownloadFailedNotification(
                                        context,
                                        false,
                                        R.string.download_error_server,
                                        R.string.download_notification_error_server
                                    )

                                    Result.failure(Data.Builder().apply {
                                        putInt(WorkDataDownloadFailureType, DownloadFailure.ServerError.value)
                                    }.build())
                                }
                            }

                            e is IOException -> if (retryCount <= 5) {
                                logDebug(TAG, "IOException encountered. Retrying ($retryCount/5)")
                                Result.retry()
                            } else {
                                showDownloadFailedNotification(
                                    context,
                                    false,
                                    R.string.download_error_server,
                                    R.string.download_notification_error_internal
                                )

                                Result.failure(Data.Builder().apply {
                                    putInt(WorkDataDownloadFailureType, DownloadFailure.ConnectionError.value)
                                }.build())
                            }

                            else -> {
                                // Delete any associated tracker preferences to allow retrying this work with a fresh state
                                PrefManager.remove(KeyDownloadBytesDone)

                                // Try deleting the file to allow retrying this work with a fresh state
                                // Even if it doesn't get deleted, we can overwrite data to the same file
                                if (!tempFile.delete()) logWarning(TAG, "Could not delete the partially downloaded ZIP")

                                Result.failure(Data.Builder().apply {
                                    putInt(WorkDataDownloadFailureType, DownloadFailure.Unknown.value)
                                }.build())
                            }
                        }
                    }
                }

                logDebug(TAG, "Download completed. Deleting any associated tracker preferences")
                PrefManager.remove(KeyDownloadBytesDone)

                setProgress(Data.Builder().apply {
                    putLong(WorkDataDownloadBytesDone, contentLength)
                    putLong(WorkDataDownloadTotalBytes, contentLength)
                    putInt(WorkDataDownloadProgress, 100)
                }.build())

                // Copy file to root directory of internal storage
                // Note: this requires the storage permission to be granted
                try {
                    moveTempFileToCorrectLocation()
                } catch (e: IOException) {
                    logError(TAG, "Could not rename file", e)

                    return@withContext Result.failure(Data.Builder().apply {
                        putInt(WorkDataDownloadFailureType, DownloadFailure.CouldNotMoveTempFile.value)
                    }.build())
                }
            }
        }

        enqueueVerificationWork()

        Result.success()
    }

    private fun enqueueVerificationWork() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Since download has completed successfully, we can start the verification work immediately
            context.showToast(R.string.download_verifying_start)
        }, 0)

        val verificationWorkRequest = OneTimeWorkRequestBuilder<Md5VerificationWorker>()
            .setInputData(Data.Builder().apply {
                putString(Md5VerificationWorker.FILENAME, updateData!!.filename!!)
                putString(Md5VerificationWorker.MD5, updateData.md5sum)
            }.build())
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            ).build()

        workManager.enqueueUniqueWork(
            WorkUniqueMd5Verification,
            ExistingWorkPolicy.REPLACE,
            verificationWorkRequest
        )
    }

    /**
     * Moves the temporary downloaded file to the correct location (root directory of internal storage).
     *
     * @throws IOException if [zipFile] exists and can't be deleted, or if [tempFile] can't be renamed to [zipFile]
     * @see File.renameTo
     */
    @Throws(IOException::class)
    private fun moveTempFileToCorrectLocation() {
        if (zipFile.exists() && !zipFile.delete()) throw IOException("Deletion Failed")

        // Move operation: rename `tempFile` to `zipFile`
        if (!tempFile.renameTo(zipFile)) {
            // If the file couldn't be renamed, copy it.
            // An [IOException] will be thrown if this fails as well
            tempFile.copyTo(zipFile)
        }

        // Delete `tempFile`, if it still exists after the rename operation
        if (tempFile.exists() && zipFile.exists()) tempFile.delete()
    }

    /**
     * Publish progress if at least [MinMsBetweenProgressPublish] milliseconds have passed since the previous event.
     *
     * Additionally, we're calling [setProgress] only if this worker is not in the stopped state.
     *
     * @param bytesDone bytes read so far, including any bytes previously read in paused/cancelled download for the same file
     * @param totalBytes total bytes that need to be read
     */
    private suspend fun publishProgressIfNeeded(bytesDone: Long, totalBytes: Long) = withContext(Dispatchers.IO) {
        if (isStopped) return@withContext

        val currentTimestamp = System.currentTimeMillis()
        if (isFirstPublish || currentTimestamp - previousProgressTimestamp > MinMsBetweenProgressPublish) {
            val progress = (bytesDone * 100 / totalBytes).toInt()
            val previousBytesDone = PrefManager.getLong(KeyDownloadBytesDone, NotSetL)

            PrefManager.putLong(KeyDownloadBytesDone, bytesDone)

            val downloadEta = calculateDownloadEta(
                currentTimestamp,
                previousBytesDone,
                bytesDone,
                totalBytes
            )?.toString(context)

            trySetForeground(createProgressNotification(bytesDone, totalBytes, progress, downloadEta))
            setProgress(Data.Builder().apply {
                putLong(WorkDataDownloadBytesDone, bytesDone)
                putLong(WorkDataDownloadTotalBytes, totalBytes)
                putInt(WorkDataDownloadProgress, progress)
                putString(WorkDataDownloadEta, downloadEta)
            }.build())

            if (!isFirstPublish) previousProgressTimestamp = currentTimestamp

            isFirstPublish = false
        }
    }

    private fun calculateDownloadEta(
        currentTimestamp: Long,
        previousBytesDone: Long,
        bytesDone: Long,
        totalBytes: Long,
    ): TimeRemaining? {
        val bytesDonePerSecond: Double
        var secondsRemaining = NotSetL

        if (previousBytesDone != NotSetL) {
            val secondsElapsed = TimeUnit.MILLISECONDS.toSeconds(currentTimestamp - previousProgressTimestamp)
            bytesDonePerSecond = if (secondsElapsed > 0L) {
                (bytesDone - previousBytesDone) / secondsElapsed.toDouble()
            } else 0.0

            // Sometimes no new progress data is available.
            // If no new data is available, return the previously stored data to keep the UI showing that.
            val validMeasurement = bytesDonePerSecond > 0 || secondsElapsed > 5

            if (validMeasurement) {
                // In case of no network, clear all measurements to allow displaying the now-unknown ETA
                if (bytesDonePerSecond == 0.0) measurements.clear()

                // Remove old measurements to keep the average calculation based on 5 measurements
                if (measurements.size > 10) measurements.subList(0, 1).clear()

                measurements.add(bytesDonePerSecond)
            }

            // Calculate number of seconds remaining based off average download speed
            val averageBytesPerSecond = if (measurements.isEmpty()) 0L else (measurements.sum() / measurements.size).toLong()
            secondsRemaining = if (averageBytesPerSecond > 0) {
                (totalBytes - bytesDone) / averageBytesPerSecond
            } else NotSetL
        }

        return if (secondsRemaining != NotSetL) TimeRemaining(secondsRemaining) else null
    }

    companion object {
        private const val TAG = "DownloadWorker"

        /**
         * Minimum difference between file size reported by `body.contentLength()` and what's saved in our backend.
         * Currently: `1 MB`
         */
        private const val ThresholdBytesDifferenceWithBackend = 1048576L
    }
}
