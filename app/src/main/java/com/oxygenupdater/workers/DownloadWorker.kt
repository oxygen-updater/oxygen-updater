package com.oxygenupdater.workers

import android.app.Notification.CATEGORY_PROGRESS
import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.oxygenupdater.R
import com.oxygenupdater.apis.DownloadApi
import com.oxygenupdater.enums.DownloadFailure
import com.oxygenupdater.exceptions.OxygenUpdaterException
import com.oxygenupdater.extensions.createFromWorkData
import com.oxygenupdater.extensions.formatFileSize
import com.oxygenupdater.extensions.toWorkData
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.internal.settings.SettingsManager.PROPERTY_DOWNLOAD_BYTES_DONE
import com.oxygenupdater.models.TimeRemaining
import com.oxygenupdater.utils.ExceptionUtils
import com.oxygenupdater.utils.LocalNotifications.showDownloadFailedNotification
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logInfo
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationIds
import com.oxygenupdater.utils.UpdateDataVersionFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.getKoin
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Handles downloading ZIPs from OnePlus OTA servers
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class DownloadWorker(
    private val context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private var isFirstPublish = true
    private var previousProgressTimestamp = 0L
    private val measurements = ArrayList<Double>()
    private val updateData = createFromWorkData(parameters.inputData)

    private lateinit var tempFile: File
    private lateinit var zipFile: File

    private val downloadApi by getKoin().inject<DownloadApi>()
    private val workManager by getKoin().inject<WorkManager>()
    private val notificationManager by getKoin().inject<NotificationManagerCompat>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Mark the Worker as important
        setForeground(createInitialForegroundInfo())

        when {
            updateData?.downloadUrl == null -> {
                showDownloadFailedNotification(context, false, R.string.download_error_internal, R.string.download_notification_error_internal)

                Result.failure(
                    workDataOf(
                        WORK_DATA_DOWNLOAD_FAILURE_TYPE to DownloadFailure.NULL_UPDATE_DATA_OR_DOWNLOAD_URL.name
                    )
                )
            }
            updateData.downloadUrl?.contains("http") == false -> {
                showDownloadFailedNotification(context, false, R.string.download_error_internal, R.string.download_notification_error_internal)

                Result.failure(
                    workDataOf(
                        WORK_DATA_DOWNLOAD_FAILURE_TYPE to DownloadFailure.DOWNLOAD_URL_INVALID_SCHEME.name
                    )
                )
            }
            else -> download()
        }
    }

    private fun createInitialForegroundInfo(): ForegroundInfo {
        // This PendingIntent can be used to cancel the worker
        val cancelPendingIntent = workManager.createCancelPendingIntent(id)

        val text = context.getString(R.string.download_pending)

        val notification = NotificationCompat.Builder(context, DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID)
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

        return ForegroundInfo(NotificationIds.LOCAL_DOWNLOAD_FOREGROUND, notification)
    }

    private fun createProgressForegroundInfo(
        bytesDone: Long,
        totalBytes: Long,
        progress: Int,
        downloadEta: String?
    ): ForegroundInfo {
        // This PendingIntent can be used to cancel the worker
        val cancelPendingIntent = workManager.createCancelPendingIntent(id)

        val bytesDoneStr = context.formatFileSize(bytesDone)
        val totalBytesStr = context.formatFileSize(totalBytes)

        val text = "$bytesDoneStr / $totalBytesStr ($progress%)"

        val notification = NotificationCompat.Builder(context, DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID)
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
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setSummaryText(downloadEta ?: "")
            )
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .build()

        notificationManager.apply {
            cancel(NotificationIds.LOCAL_DOWNLOAD)
            cancel(NotificationIds.LOCAL_MD5_VERIFICATION)
        }

        return ForegroundInfo(NotificationIds.LOCAL_DOWNLOAD_FOREGROUND, notification)
    }

    private suspend fun download(): Result = withContext(Dispatchers.IO) {
        tempFile = File(context.getExternalFilesDir(null), updateData!!.filename!!)
        @Suppress("DEPRECATION")
        zipFile = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).absolutePath, updateData.filename!!)

        var startingByte = SettingsManager.getPreference<Long?>(PROPERTY_DOWNLOAD_BYTES_DONE, null)
        var rangeHeader = if (startingByte != null) "bytes=$startingByte-" else null

        if (startingByte != null) {
            logDebug(TAG, "Looks like a resume operation. Adding $rangeHeader to the request")

            if (tempFile.length() > startingByte) {
                logWarning(
                    TAG,
                    "Partially downloaded ZIP size differs from skipped bytes (${tempFile.length()} vs $startingByte)."
                )
            } else if (tempFile.length() < startingByte) {
                logWarning(
                    TAG,
                    OxygenUpdaterException("Partially downloaded ZIP size (${tempFile.length()}) is lesser than skipped bytes ($startingByte). Resetting state.")
                )

                startingByte = null
                rangeHeader = null
            }
        }

        val response = downloadApi.downloadZip(
            updateData.downloadUrl!!,
            rangeHeader
        )

        val body = response.body()

        if (!response.isSuccessful || body == null) {
            return@withContext Result.failure(
                workDataOf(
                    WORK_DATA_DOWNLOAD_FAILURE_TYPE to DownloadFailure.UNSUCCESSFUL_RESPONSE.name,
                    WORK_DATA_DOWNLOAD_FAILURE_EXTRA_URL to updateData.downloadUrl,
                    WORK_DATA_DOWNLOAD_FAILURE_EXTRA_FILENAME to updateData.filename,
                    WORK_DATA_DOWNLOAD_FAILURE_EXTRA_VERSION to updateData.versionNumber,
                    WORK_DATA_DOWNLOAD_FAILURE_EXTRA_OTA_VERSION to updateData.otaVersionNumber,
                    WORK_DATA_DOWNLOAD_FAILURE_EXTRA_HTTP_CODE to response.code(),
                    WORK_DATA_DOWNLOAD_FAILURE_EXTRA_HTTP_MESSAGE to response.message()
                )
            )
        }

        body.byteStream().use { stream ->
            logInfo(TAG, "Downloading ZIP from ${startingByte ?: 0} bytes")

            // Copy stream to file
            // We could have used the [InputStream.copyTo] extension defined in [IOStreams.kt],
            // but we need to support pause/resume functionality, as well as publish progress
            RandomAccessFile(tempFile, "rw").use { randomAccessFile ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes: Int
                var bytesRead = startingByte ?: 0L
                val contentLength = bytesRead + body.contentLength()

                if (abs(contentLength - updateData.downloadSize) > THRESHOLD_BYTES_DIFFERENCE_WITH_BACKEND) {
                    logWarning(
                        TAG,
                        "Content length reported by the download server differs from UpdateData.downloadSize ($contentLength vs ${updateData.downloadSize})"
                    )
                }

                if (startingByte != null) {
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

                                    Result.failure(
                                        workDataOf(
                                            WORK_DATA_DOWNLOAD_FAILURE_TYPE to DownloadFailure.SERVER_ERROR.name
                                        )
                                    )
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

                                Result.failure(
                                    workDataOf(
                                        WORK_DATA_DOWNLOAD_FAILURE_TYPE to DownloadFailure.CONNECTION_ERROR.name
                                    )
                                )
                            }
                            else -> {
                                // Delete any associated tracker preferences to allow retrying this work with a fresh state
                                SettingsManager.removePreference(PROPERTY_DOWNLOAD_BYTES_DONE)

                                // Try deleting the file to allow retrying this work with a fresh state
                                // Even if it doesn't get deleted, we can overwrite data to the same file
                                if (!tempFile.delete()) {
                                    logWarning(TAG, "Could not delete the partially downloaded ZIP")
                                }

                                Result.failure(
                                    workDataOf(
                                        WORK_DATA_DOWNLOAD_FAILURE_TYPE to DownloadFailure.UNKNOWN.name
                                    )
                                )
                            }
                        }
                    }
                }

                logDebug(TAG, "Download completed. Deleting any associated tracker preferences")
                SettingsManager.removePreference(PROPERTY_DOWNLOAD_BYTES_DONE)

                setProgress(
                    workDataOf(
                        WORK_DATA_DOWNLOAD_BYTES_DONE to contentLength,
                        WORK_DATA_DOWNLOAD_TOTAL_BYTES to contentLength,
                        WORK_DATA_DOWNLOAD_PROGRESS to 100
                    )
                )

                // Copy file to root directory of internal storage
                // Note: this requires the storage permission to be granted
                try {
                    moveTempFileToCorrectLocation()
                } catch (e: IOException) {
                    logError(TAG, "Could not rename file", e)

                    return@withContext Result.failure(
                        workDataOf(
                            WORK_DATA_DOWNLOAD_FAILURE_TYPE to DownloadFailure.COULD_NOT_MOVE_TEMP_FILE.name
                        )
                    )
                }
            }
        }

        enqueueVerificationWork()

        Result.success()
    }

    private fun enqueueVerificationWork() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Since download has completed successfully, we can start the verification work immediately
            Toast.makeText(
                context,
                context.getString(R.string.download_verifying_start),
                Toast.LENGTH_LONG
            ).show()
        }, 0)

        val verificationWorkRequest = OneTimeWorkRequestBuilder<Md5VerificationWorker>()
            .setInputData(updateData!!.toWorkData())
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            ).build()

        workManager.enqueueUniqueWork(
            WORK_UNIQUE_MD5_VERIFICATION,
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
        if (zipFile.exists() && !zipFile.delete()) {
            throw IOException("Deletion Failed")
        }

        // Move operation: rename `tempFile` to `zipFile`
        if (!tempFile.renameTo(zipFile)) {
            // If the file couldn't be renamed, copy it.
            // An [IOException] will be thrown if this fails as well
            tempFile.copyTo(zipFile)
        }

        // Delete `tempFile`, if it still exists after the rename operation
        if (tempFile.exists() && zipFile.exists()) {
            tempFile.delete()
        }
    }

    /**
     * Publish progress if at least [THRESHOLD_PUBLISH_PROGRESS_TIME_PASSED] milliseconds have passed since the previous event.
     *
     * Additionally, we're calling [setProgress] only if this worker is not in the stopped state.
     *
     * @param bytesDone bytes read so far, including any bytes previously read in paused/cancelled download for the same file
     * @param totalBytes total bytes that need to be read
     */
    private suspend fun publishProgressIfNeeded(bytesDone: Long, totalBytes: Long) = withContext(Dispatchers.IO) {
        if (!isStopped) {
            val currentTimestamp = System.currentTimeMillis()
            if (isFirstPublish || currentTimestamp - previousProgressTimestamp > THRESHOLD_PUBLISH_PROGRESS_TIME_PASSED) {
                val progress = (bytesDone * 100 / totalBytes).toInt()
                val previousBytesDone = SettingsManager.getPreference(PROPERTY_DOWNLOAD_BYTES_DONE, NOT_SET)

                SettingsManager.savePreference(PROPERTY_DOWNLOAD_BYTES_DONE, bytesDone)

                val downloadEta = calculateDownloadEta(
                    currentTimestamp,
                    previousBytesDone,
                    bytesDone,
                    totalBytes
                )?.toString(applicationContext)

                setForeground(
                    createProgressForegroundInfo(
                        bytesDone,
                        totalBytes,
                        progress,
                        downloadEta
                    )
                )
                setProgress(
                    workDataOf(
                        WORK_DATA_DOWNLOAD_BYTES_DONE to bytesDone,
                        WORK_DATA_DOWNLOAD_TOTAL_BYTES to totalBytes,
                        WORK_DATA_DOWNLOAD_PROGRESS to progress,
                        WORK_DATA_DOWNLOAD_ETA to downloadEta
                    )
                )

                if (!isFirstPublish) {
                    previousProgressTimestamp = currentTimestamp
                }

                isFirstPublish = false
            }
        }
    }

    private fun calculateDownloadEta(
        currentTimestamp: Long,
        previousBytesDone: Long,
        bytesDone: Long,
        totalBytes: Long
    ): TimeRemaining? {
        val bytesDonePerSecond: Double
        var secondsRemaining = NOT_SET

        if (previousBytesDone != NOT_SET) {
            val secondsElapsed = TimeUnit.MILLISECONDS.toSeconds(currentTimestamp - previousProgressTimestamp)
            bytesDonePerSecond = if (secondsElapsed > 0L) {
                (bytesDone - previousBytesDone) / secondsElapsed.toDouble()
            } else {
                0.0
            }

            // Sometimes no new progress data is available.
            // If no new data is available, return the previously stored data to keep the UI showing that.
            val validMeasurement = bytesDonePerSecond > 0 || secondsElapsed > 5

            if (validMeasurement) {
                // In case of no network, clear all measurements to allow displaying the now-unknown ETA...
                if (bytesDonePerSecond == 0.0) {
                    measurements.clear()
                }

                // Remove old measurements to keep the average calculation based on 5 measurements
                if (measurements.size > 10) {
                    measurements.subList(0, 1).clear()
                }

                measurements.add(bytesDonePerSecond)
            }

            // Calculate number of seconds remaining based off average download speed
            val averageBytesPerSecond = if (measurements.isNullOrEmpty()) {
                0L
            } else {
                (measurements.sum() / measurements.size).toLong()
            }

            secondsRemaining = if (averageBytesPerSecond > 0) {
                (totalBytes - bytesDone) / averageBytesPerSecond
            } else {
                NOT_SET
            }
        }

        return if (secondsRemaining != NOT_SET) {
            TimeRemaining(secondsRemaining)
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "DownloadWorker"

        private const val NOT_SET = -1L

        /**
         * Minimum difference between file size reported by `body.contentLength()` and what's saved in our backend.
         * Currently: `1 MB`
         */
        private const val THRESHOLD_BYTES_DIFFERENCE_WITH_BACKEND = 1048576L
    }
}
