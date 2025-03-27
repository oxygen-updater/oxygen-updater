package com.oxygenupdater.workers

import android.app.Notification
import android.app.Notification.CATEGORY_PROGRESS
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.R
import com.oxygenupdater.apis.DownloadApi
import com.oxygenupdater.extensions.formatFileSize
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.remove
import com.oxygenupdater.extensions.set
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.internal.NotSet
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyDownloadBytesDone
import com.oxygenupdater.models.TimeRemaining
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.update.DownloadFailure
import com.oxygenupdater.ui.update.DownloadStatus
import com.oxygenupdater.ui.update.Md5VerificationFailure
import com.oxygenupdater.utils.LocalNotifications
import com.oxygenupdater.utils.LocalNotifications.showDownloadFailedNotification
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.DownloadStatusNotifChannelId
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.VerificationStatusNotifChannelId
import com.oxygenupdater.utils.NotificationIds
import com.oxygenupdater.utils.UpdateDataVersionFormatter
import com.oxygenupdater.utils.isNetworkError
import com.oxygenupdater.utils.logDebug
import com.oxygenupdater.utils.logError
import com.oxygenupdater.utils.logInfo
import com.oxygenupdater.utils.logVerbose
import com.oxygenupdater.utils.logWarning
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.math.BigInteger
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Handles downloading ZIPs from OnePlus/Google OTA servers, and also verifies their MD5 checksum.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted parameters: WorkerParameters,
    private val sharedPreferences: SharedPreferences,
    private val downloadApi: DownloadApi,
    private val workManager: WorkManager,
    private val crashlytics: FirebaseCrashlytics,
) : CoroutineWorker(context, parameters) {

    private var isFirstPublish = true
    private var previousProgressTimeMs = 0L
    private val measurements = ArrayList<Double>()
    private val updateData = UpdateData.createFromWorkData(parameters.inputData)

    private lateinit var tempFile: File
    private lateinit var zipFile: File
    private lateinit var notification: Notification

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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        return notification
    }

    private fun createDownloadProgressNotification(
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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        LocalNotifications.hideDownloadCompleteNotification(context)

        return notification
    }

    private fun createVerificationProgressNotification() = NotificationCompat.Builder(
        context, VerificationStatusNotifChannelId
    )
        .setSmallIcon(R.drawable.logo_notification)
        .setContentTitle(context.getString(R.string.download_verifying))
        .setProgress(100, 50, true)
        .setOngoing(true)
        .setCategory(CATEGORY_PROGRESS)
        .setPriority(PRIORITY_LOW)
        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()

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
        crashlytics.logError(TAG, "setForeground failed", e)
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

        var startingByte = sharedPreferences[KeyDownloadBytesDone, NotSetL]
        var rangeHeader = if (startingByte != NotSetL) "bytes=$startingByte-" else null

        if (startingByte != NotSetL) {
            logDebug(TAG, "Looks like a resume operation. Adding $rangeHeader to the request")

            if (tempFile.length() > startingByte) crashlytics.logWarning(
                TAG,
                "Partially downloaded ZIP size differs from skipped bytes (${tempFile.length()} vs $startingByte)."
            ) else if (tempFile.length() < startingByte) {
                crashlytics.logWarning(
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

                if (abs(contentLength - updateData.downloadSize) > ThresholdBytesDifferenceWithBackend) {
                    crashlytics.logWarning(
                        TAG,
                        "Content length reported by the download server differs from UpdateData.downloadSize ($contentLength vs ${updateData.downloadSize})"
                    )
                }

                if (startingByte != NotSetL) {
                    logDebug(TAG, "Looks like a resume operation. Seeking to $startingByte bytes")
                    randomAccessFile.seek(startingByte)
                }

                try {
                    while (stream.read(buffer).also { bytes = it } >= 0) {
                        randomAccessFile.write(buffer, 0, bytes)
                        bytesRead += bytes

                        publishDownloadProgress(bytesDone = bytesRead, totalBytes = contentLength)
                    }
                } catch (e: Exception) {
                    return@withContext if (isStopped) {
                        logDebug(TAG, "Ignoring exception since worker is in stopped state: ${e.message}")
                        // Mark success as download completed only
                        Result.success()
                    } else {
                        crashlytics.logError(TAG, "Exception while reading from byteStream", e)

                        val retryCount = runAttemptCount + 1

                        when {
                            isNetworkError(e) -> {
                                if (retryCount <= MaxRetries) {
                                    logDebug(TAG, "Network error encountered. Retrying ($retryCount/$MaxRetries)")
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
                                        putInt(WorkDataDownloadFailureExtraHttpCode, response.code())
                                        putString(WorkDataDownloadFailureExtraHttpMessage, response.message())
                                    }.build())
                                }
                            }

                            e is IOException -> if (retryCount <= MaxRetries) {
                                logDebug(TAG, "IOException encountered. Retrying ($retryCount/$MaxRetries)")
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
                                sharedPreferences.remove(KeyDownloadBytesDone)

                                // Try deleting the file to allow retrying this work with a fresh state
                                // Even if it doesn't get deleted, we can overwrite data to the same file
                                if (!tempFile.delete()) crashlytics.logWarning(
                                    TAG, "Could not delete the partially downloaded ZIP"
                                )

                                Result.failure(Data.Builder().apply {
                                    putInt(WorkDataDownloadFailureType, DownloadFailure.Unknown.value)
                                }.build())
                            }
                        }
                    }
                }

                logDebug(TAG, "Download completed. Deleting any associated tracker preferences")
                sharedPreferences.remove(KeyDownloadBytesDone)

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
                    crashlytics.logError(TAG, "Could not rename file", e)

                    return@withContext Result.failure(Data.Builder().apply {
                        putInt(WorkDataDownloadFailureType, DownloadFailure.CouldNotMoveTempFile.value)
                    }.build())
                }
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // Since download has completed successfully, we can start the verification work immediately
            context.showToast(R.string.download_verifying_start)
        }, 0)

        LocalNotifications.showVerifyingNotification(context)
        publishVerificationProgress()
        verify()
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
            /** Rename failed; try copying. An [IOException] will be thrown if this fails as well. */
            try {
                tempFile.copyTo(zipFile)
            } catch (e: NoSuchFileException) {
                /**
                 * [NoSuchFileException] => tempFile doesn't exist. If `zipFile` exists, then
                 * the previous [File.renameTo] command succeeded despite returning false.
                 *
                 * Otherwise wrap in [IOException] and rethrow so that worker can fail.
                 */
                if (!zipFile.exists()) throw IOException(e)
            }
        }

        // Delete `tempFile`, if it still exists after the rename operation
        if (tempFile.exists() && zipFile.exists()) tempFile.delete()
    }

    private suspend fun verify() = withContext(Dispatchers.IO) {
        if (updateData == null) {
            crashlytics.logWarning(TAG, "updateData = null")
            return@withContext Result.failure(Data.Builder().apply {
                putInt(WorkDataMd5VerificationFailureType, Md5VerificationFailure.NullUpdateData.value)
            }.build())
        }

        val filename = updateData.filename
        val md5 = updateData.md5sum
        if (filename.isNullOrEmpty() || md5.isNullOrEmpty()) {
            crashlytics.logWarning(TAG, "Required parameters are null/empty: filename=$filename, md5=$md5")
            return@withContext Result.failure(Data.Builder().apply {
                putInt(WorkDataMd5VerificationFailureType, Md5VerificationFailure.EmptyFilenameOrMd5.value)
            }.build())
        }

        logDebug(TAG, "Verifying $filename")
        val calculatedDigest = calculateMd5()
        if (calculatedDigest == null) {
            crashlytics.logWarning(TAG, "calculatedDigest = null")
            return@withContext Result.failure(Data.Builder().apply {
                putInt(WorkDataMd5VerificationFailureType, Md5VerificationFailure.NullCalculatedChecksum.value)
            }.build())
        }

        logVerbose(TAG, "Calculated digest: $calculatedDigest")
        logVerbose(TAG, "Provided digest: $md5")

        if (calculatedDigest.equals(md5, ignoreCase = true)) {
            LocalNotifications.showDownloadCompleteNotification(context)
            // Mark success as verification completed
            Result.success(Data.Builder().apply {
                putInt(WorkDataDownloadSuccessType, DownloadStatus.VerificationCompleted.value)
            }.build())
        } else {
            LocalNotifications.showVerificationFailedNotification(context)
            crashlytics.logWarning(TAG, "md5 != calculatedDigest")
            Result.failure(Data.Builder().apply {
                putInt(WorkDataMd5VerificationFailureType, Md5VerificationFailure.ChecksumsNotEqual.value)
            }.build())
        }
    }

    private suspend fun calculateMd5(): String? = withContext(Dispatchers.IO) {
        val digest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            crashlytics.logError(TAG, "Exception while getting digest", e)
            return@withContext null
        }

        var retryCount = 0
        while (!zipFile.exists()) {
            if (retryCount >= 3) {
                crashlytics.logError(
                    TAG,
                    "File doesn't exist, even after retrying thrice every 2s",
                    FileNotFoundException("File doesn't exist, even after retrying thrice every 2s")
                )

                return@withContext null
            }

            val suffix = "after 2s (${2 - retryCount} retries left)"
            crashlytics.logWarning(
                TAG, "File doesn't exist yet, retrying $suffix"
            )

            retryCount++

            // If the downloaded file isn't accessible accessible yet
            // (because it's still being flushed or previously-existing files are being rotated),
            // wait a bit and check again
            try {
                delay(2000)
            } catch (e: CancellationException) {
                crashlytics.logError(TAG, "Error while trying to re-verify file $suffix", e)
            }
        }

        DigestInputStream(zipFile.inputStream(), digest).use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

            return@withContext try {
                while (stream.read(buffer, 0, buffer.size) > 0) {
                    publishVerificationProgress()
                }

                // Complete hash computation
                val md5sum = digest.digest()
                // Convert to a hexadecimal string
                val bigIntStr = BigInteger(1, md5sum).toString(16)

                // Fill to 32 characters
                String.format("%32s", bigIntStr).replace(' ', '0')
            } catch (e: IOException) {
                throw RuntimeException("Unable to process file for MD5", e)
            }
        }
    }

    /**
     * @param bytesDone bytes read so far, including any bytes previously read in paused/cancelled download for the same file
     * @param totalBytes total bytes that need to be read
     */
    private suspend fun publishDownloadProgress(
        bytesDone: Long,
        totalBytes: Long,
    ) = publishProgressIfNeeded { currentTimeMs ->
        val progress = (bytesDone * 100 / totalBytes.coerceAtLeast(1L)).toInt()
        val previousBytesDone = sharedPreferences[KeyDownloadBytesDone, NotSetL]

        sharedPreferences[KeyDownloadBytesDone] = bytesDone

        val downloadEta = calculateDownloadEta(
            currentTimeMs = currentTimeMs,
            previousBytesDone = previousBytesDone,
            bytesDone = bytesDone,
            totalBytes = totalBytes,
        )?.toString(context)

        trySetForeground(createDownloadProgressNotification(bytesDone, totalBytes, progress, downloadEta))
        setProgress(Data.Builder().apply {
            putLong(WorkDataDownloadBytesDone, bytesDone)
            putLong(WorkDataDownloadTotalBytes, totalBytes)
            putInt(WorkDataDownloadProgress, progress)
            putString(WorkDataDownloadEta, downloadEta)
        }.build())
    }

    /**
     * Publish progress if at least [MinMsBetweenProgressPublish] milliseconds have passed since the previous event.
     *
     * Additionally, we're calling [setProgress] only if this worker is not in the stopped state.
     */
    private suspend fun publishVerificationProgress() = publishProgressIfNeeded {
        trySetForeground(createVerificationProgressNotification())
        setProgress(Data.Builder().apply {
            putInt(WorkDataDownloadProgress, NotSet)
        }.build())
    }

    /**
     * Publish progress if at least [MinMsBetweenProgressPublish] milliseconds have passed since the previous event.
     *
     * Additionally, we're calling [setProgress] only if this worker is not in the stopped state.
     */
    private suspend inline fun publishProgressIfNeeded(
        crossinline block: suspend (currentTimeMs: Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (isStopped) return@withContext

        val currentTimeMs = System.currentTimeMillis()
        if (!isFirstPublish && currentTimeMs - previousProgressTimeMs <= MinMsBetweenProgressPublish) return@withContext

        block(currentTimeMs)

        if (!isFirstPublish) previousProgressTimeMs = currentTimeMs
        isFirstPublish = false
    }

    private fun calculateDownloadEta(
        currentTimeMs: Long,
        previousBytesDone: Long,
        bytesDone: Long,
        totalBytes: Long,
    ): TimeRemaining? {
        val bytesDonePerSecond: Double
        var secondsRemaining = NotSetL

        if (previousBytesDone != NotSetL) {
            val secondsElapsed = TimeUnit.MILLISECONDS.toSeconds(currentTimeMs - previousProgressTimeMs)
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
        const val FILENAME = "filename"
        const val MD5 = "md5"

        /**
         * Minimum difference between file size reported by `body.contentLength()` and what's saved in our backend.
         * Currently: `1 MB`
         */
        private const val ThresholdBytesDifferenceWithBackend = 1048576L
        private const val MaxRetries = 3
    }
}
