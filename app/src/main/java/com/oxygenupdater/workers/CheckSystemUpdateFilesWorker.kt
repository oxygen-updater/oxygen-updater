package com.oxygenupdater.workers

import android.content.Context
import android.os.Environment
import androidx.core.os.bundleOf
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.analytics.FirebaseAnalytics
import com.oxygenupdater.database.SubmittedUpdateFileRepository
import com.oxygenupdater.exceptions.NetworkException
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.internal.settings.SettingsManager.Companion.PROPERTY_CONTRIBUTION_COUNT
import com.oxygenupdater.models.ServerPostResult
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.LocalNotifications
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logInfo
import com.oxygenupdater.utils.Logger.logWarning
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

    private val analytics by inject(FirebaseAnalytics::class.java)
    private val serverRepository by inject(ServerRepository::class.java)
    private val settingsManager by inject(SettingsManager::class.java)

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        logDebug(TAG, "Started update file check")

        var anySubmitFailed = false
        val validSubmittedFileNames = HashSet<String>()

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
                            analytics.logEvent(
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
                            validSubmittedFileNames.add(fileName)

                            // Log successful contribution
                            analytics.logEvent(
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

        val count = validSubmittedFileNames.size
        if (count != 0) {
            LocalNotifications.showContributionSuccessfulNotification(
                context,
                validSubmittedFileNames
            )

            // Increase number of submitted updates. Not currently shown in the UI, but may come in handy later.
            settingsManager.savePreference(
                PROPERTY_CONTRIBUTION_COUNT,
                settingsManager.getPreference(PROPERTY_CONTRIBUTION_COUNT, 0) + count
            )
        }

        repository.close()
        if (anySubmitFailed) Result.failure() else Result.success()
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
