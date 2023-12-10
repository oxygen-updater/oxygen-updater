package com.oxygenupdater.workers

import android.content.Context
import android.os.Environment
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.ui.update.Md5VerificationFailure
import com.oxygenupdater.utils.LocalNotifications
import com.oxygenupdater.utils.logDebug
import com.oxygenupdater.utils.logError
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
import java.math.BigInteger
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Handles MD5 checksum verification on downloaded ZIPs
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@HiltWorker
class Md5VerificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted parameters: WorkerParameters,
    private val crashlytics: FirebaseCrashlytics,
) : CoroutineWorker(context, parameters) {

    private val filename: String?
    private val md5: String?

    init {
        val inputData = parameters.inputData

        filename = inputData.getString(FILENAME)
        md5 = inputData.getString(MD5)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        LocalNotifications.showVerifyingNotification(context)

        verify()
    }

    private suspend fun verify(): Result = withContext(Dispatchers.IO) {
        if (filename == null || md5 == null) {
            crashlytics.logWarning(TAG, "Required parameters are null: $filename, $md5")
            Result.failure(Data.Builder().apply {
                putInt(WorkDataMd5VerificationFailureType, Md5VerificationFailure.NullUpdateData.value)
            }.build())
        } else {
            logDebug(TAG, "Verifying $filename")

            if (md5.isEmpty()) {
                crashlytics.logWarning(TAG, "MD5 is empty")
                Result.failure(Data.Builder().apply {
                    putInt(WorkDataMd5VerificationFailureType, Md5VerificationFailure.NullOrEmptyProvidedChecksum.value)
                }.build())
            } else {
                val calculatedDigest = calculateMd5()
                if (calculatedDigest == null) {
                    crashlytics.logWarning(TAG, "calculatedDigest = null")
                    Result.failure(Data.Builder().apply {
                        putInt(WorkDataMd5VerificationFailureType, Md5VerificationFailure.NullCalculatedChecksum.value)
                    }.build())
                } else {
                    logVerbose(TAG, "Calculated digest: $calculatedDigest")
                    logVerbose(TAG, "Provided digest: $md5")

                    if (calculatedDigest.equals(md5, ignoreCase = true)) {
                        LocalNotifications.showDownloadCompleteNotification(context)
                        Result.success()
                    } else {
                        LocalNotifications.showVerificationFailedNotification(context)
                        crashlytics.logWarning(TAG, "md5 != calculatedDigest")
                        Result.failure(Data.Builder().apply {
                            putInt(WorkDataMd5VerificationFailureType, Md5VerificationFailure.ChecksumsNotEqual.value)
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
            crashlytics.logError(TAG, "Exception while getting digest", e)
            return@withContext null
        }

        val zipFile = File(Environment.getExternalStoragePublicDirectory(DirectoryRoot).absolutePath, filename!!)

        var retryCount = 0
        while (!zipFile.exists()) {
            if (retryCount >= 5) {
                crashlytics.logError(
                    TAG,
                    "File doesn't exist, even after retrying every 2 seconds upto 5 times",
                    FileNotFoundException("File doesn't exist, even after retrying every 2 seconds upto 5 times")
                )

                return@withContext null
            }

            crashlytics.logWarning(
                TAG, "File doesn't exist yet, retrying after 2 seconds (${4 - retryCount} retries left)"
            )

            retryCount++

            // If the downloaded file isn't accessible accessible yet
            // (because it's still being flushed or previously-existing files are being rotated),
            // wait a bit and check again
            try {
                delay(2000)
            } catch (e: CancellationException) {
                crashlytics.logError(
                    TAG, "Error while trying to re-verify file after 2 seconds (${4 - retryCount} retries left)", e
                )
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
        const val FILENAME = "filename"
        const val MD5 = "md5"
    }
}
