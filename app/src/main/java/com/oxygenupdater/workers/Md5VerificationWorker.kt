package com.oxygenupdater.workers

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.oxygenupdater.compose.ui.update.Md5VerificationFailure
import com.oxygenupdater.exceptions.UpdateVerificationException
import com.oxygenupdater.extensions.attachWithLocale
import com.oxygenupdater.utils.LocalNotifications
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logVerbose
import com.oxygenupdater.utils.Logger.logWarning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.math.BigInteger
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Handles MD5 checksum verification on downloaded ZIPs
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class Md5VerificationWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    private val context = context.attachWithLocale(false)

    private val filename: String?
    private val md5: String?

    init {
        val inputData = parameters.inputData

        filename = inputData.getString("filename")
        md5 = inputData.getString("md5")
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        LocalNotifications.showVerifyingNotification(context)

        verify()
    }

    private suspend fun verify(): Result = withContext(Dispatchers.IO) {
        if (filename == null || md5 == null) {
            logError(TAG, UpdateVerificationException("updateData = null"))
            Result.failure(Data.Builder().apply {
                putInt(WORK_DATA_MD5_VERIFICATION_FAILURE_TYPE, Md5VerificationFailure.NullUpdateData.value)
            }.build())
        } else {
            logDebug(TAG, "Verifying $filename")

            if (md5.isEmpty()) {
                logError(TAG, UpdateVerificationException("updateData.mD5Sum = null/empty"))
                Result.failure(Data.Builder().apply {
                    putInt(WORK_DATA_MD5_VERIFICATION_FAILURE_TYPE, Md5VerificationFailure.NullOrEmptyProvidedChecksum.value)
                }.build())
            } else {
                val calculatedDigest = calculateMd5()
                if (calculatedDigest == null) {
                    logError(TAG, UpdateVerificationException("calculatedDigest = null"))
                    Result.failure(Data.Builder().apply {
                        putInt(WORK_DATA_MD5_VERIFICATION_FAILURE_TYPE, Md5VerificationFailure.NullCalculatedChecksum.value)
                    }.build())
                } else {
                    logVerbose(TAG, "Calculated digest: $calculatedDigest")
                    logVerbose(TAG, "Provided digest: $md5")

                    if (calculatedDigest.equals(md5, ignoreCase = true)) {
                        LocalNotifications.showDownloadCompleteNotification(context)
                        Result.success()
                    } else {
                        LocalNotifications.showVerificationFailedNotification(context)
                        logError(TAG, UpdateVerificationException("updateData.mD5Sum != calculatedDigest"))
                        Result.failure(Data.Builder().apply {
                            putInt(WORK_DATA_MD5_VERIFICATION_FAILURE_TYPE, Md5VerificationFailure.ChecksumsNotEqual.value)
                        }.build())
                    }
                }
            }
        }
    }

    private suspend fun calculateMd5(): String? = withContext(Dispatchers.IO) {
        val digest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            logError(TAG, "Exception while getting digest", e)
            return@withContext null
        }

        val zipFile = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).absolutePath, filename!!)

        var retryCount = 0
        while (!zipFile.exists()) {
            if (retryCount >= 5) {
                logError(
                    TAG,
                    "File doesn't exist, even after retrying every 2 seconds upto 5 times",
                    FileNotFoundException("File doesn't exist, even after retrying every 2 seconds upto 5 times")
                )

                return@withContext null
            }

            logWarning(TAG, "File doesn't exist yet, retrying after 2 seconds (${4 - retryCount} retries left)")

            retryCount++

            // If the downloaded file isn't accessible accessible yet
            // (because it's still being flushed or previously-existing files are being rotated),
            // wait a bit and check again
            try {
                delay(2000)
            } catch (e: CancellationException) {
                logError(TAG, "Error while trying to re-verify file after 2 seconds (${4 - retryCount} retries left)", e)
            }
        }

        DigestInputStream(zipFile.inputStream(), digest).use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

            return@withContext try {
                while (stream.read(buffer, 0, buffer.size) > 0) {
                    // no-op
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

    companion object {
        private const val TAG = "Md5VerificationWorker"
    }
}
