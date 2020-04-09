package com.arjanvlek.oxygenupdater.workers

import android.app.Notification
import android.content.Context
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.database.SubmittedUpdateFileRepository
import com.arjanvlek.oxygenupdater.exceptions.NetworkException
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import com.arjanvlek.oxygenupdater.utils.LocalNotifications
import com.arjanvlek.oxygenupdater.utils.Logger.logDebug
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.Logger.logInfo
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import com.arjanvlek.oxygenupdater.utils.NotificationIds.FOREGROUND_NOTIFICATION_CONTRIBUTION
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.util.*

/**
 * Checks for new update files in the specified directories, and submits filenames to the backend
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class CheckSystemUpdateFilesWorker(
    private val context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val repository = SubmittedUpdateFileRepository(context)

    private val serverRepository by inject(ServerRepository::class.java)
    private val settingsManager by inject(SettingsManager::class.java)

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        // Mark the Worker as important
        setForeground(createInitialForegroundInfo())

        logDebug(TAG, "Started update file check")

        var anySubmitFailed = false
        UPDATE_DIRECTORIES.forEach { directoryName ->
            @Suppress("DEPRECATION")
            val directory = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT), directoryName)

            if (!directory.exists()) {
                logDebug(TAG, "Directory: " + directory.absolutePath + " does not exist. Skipping...")
                return@forEach
            }

            logDebug(TAG, "Started checking for update files in directory: " + directory.absolutePath)

            val fileNamesInDirectory = ArrayList<String>()
            getAllFileNames(directory, fileNamesInDirectory)

            fileNamesInDirectory.forEach { fileName ->
                logDebug(TAG, "Found update file: $fileName")

                if (!repository.isFileAlreadySubmitted(fileName)) {
                    logDebug(TAG, "Submitting update file $fileName")

                    val serverPostResult: ServerPostResult? = serverRepository.submitUpdateFile(fileName)
                    if (serverPostResult == null) {
                        // network error, try again later
                        logWarning(
                            TAG,
                            NetworkException("Error submitting update file $fileName: No network connection or empty response")
                        )
                        anySubmitFailed = true
                    } else if (!serverPostResult.success) {
                        val errorMessage = serverPostResult.errorMessage

                        // If file is already in our database or if file is an invalid temporary file (server decides when this is the case),
                        // mark this file as submitted but don't inform the user about it.
                        if (errorMessage != null && (errorMessage == FILE_ALREADY_IN_DATABASE || errorMessage == FILENAME_INVALID)) {
                            logInfo(TAG, "Ignoring submitted update file $fileName, already in database or not relevant")
                            repository.store(fileName)

                            // Log failed contribution
                            FirebaseAnalytics.getInstance(context).logEvent(
                                "CONTRIBUTION_NOT_NEEDED",
                                bundleOf("CONTRIBUTION_FILENAME" to fileName)
                            )
                        } else {
                            // server error, try again later
                            logError(
                                TAG,
                                NetworkException("Error submitting update file $fileName: ${serverPostResult.errorMessage}")
                            )
                            anySubmitFailed = true
                        }
                    } else {
                        logInfo(TAG, "Successfully submitted update file $fileName")

                        // Inform user of successful contribution (only if the file is not a "bogus" temporary file)
                        if (fileName.contains(".zip")) {
                            LocalNotifications.showContributionSuccessfulNotification(context, fileName)

                            // Increase number of submitted updates. Not currently shown in the UI, but may come in handy later.
                            settingsManager.savePreference(
                                SettingsManager.PROPERTY_CONTRIBUTION_COUNT,
                                settingsManager.getPreference(SettingsManager.PROPERTY_CONTRIBUTION_COUNT, 0) + 1
                            )

                            // Log successful contribution
                            FirebaseAnalytics.getInstance(context).logEvent(
                                "CONTRIBUTION_SUCCESSFUL",
                                bundleOf("CONTRIBUTION_FILENAME" to fileName)
                            )
                        }

                        // Store the filename in a local database to prevent re-submission until it gets installed or removed by the user.
                        repository.store(fileName)
                    }
                } else {
                    logDebug(TAG, "Update file $fileName has already been submitted. Ignoring...")
                }
            }

            logDebug(TAG, "Finished checking for update files in directory: ${directory.absolutePath}")
        }

        repository.close()
        if (anySubmitFailed) Result.failure() else Result.success()
    }

    private fun createInitialForegroundInfo(): ForegroundInfo {
        // This PendingIntent can be used to cancel the worker
        val text = context.getString(R.string.running_in_background)

        val notification = NotificationCompat.Builder(context, OxygenUpdater.PROGRESS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setTicker(text)
            .setContentText(text)
            .setSmallIcon(R.drawable.logo_outline)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(FOREGROUND_NOTIFICATION_CONTRIBUTION, notification)
    }

    private fun getAllFileNames(folder: File, result: MutableList<String>) {
        // Can be null if the user has revoked the "read external storage" permission of the app
        val files = folder.listFiles() ?: return

        logDebug(TAG, "Found ${files.size} files within the '$folder' folder")

        files.forEach {
            if (it.isDirectory) {
                getAllFileNames(it, result)
            }

            if (it.isFile) {
                logDebug(TAG, "Added $it to the list")
                result.add(it.name)
            }
        }
    }

    companion object {
        private const val TAG = "CheckSystemUpdateFilesWorker"

        private const val FILE_ALREADY_IN_DATABASE = "E_FILE_ALREADY_IN_DB"
        private const val FILENAME_INVALID = "E_FILE_INVALID"

        private val UPDATE_DIRECTORIES = arrayOf(".Ota")
    }
}
