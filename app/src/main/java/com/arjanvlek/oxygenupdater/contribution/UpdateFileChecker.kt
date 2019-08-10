package com.arjanvlek.oxygenupdater.contribution

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Bundle
import android.os.Environment
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.download.DownloadService.Companion.DIRECTORY_ROOT
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logInfo
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.internal.server.NetworkException
import com.arjanvlek.oxygenupdater.internal.server.ServerPostResult
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.google.firebase.analytics.FirebaseAnalytics
import java8.util.function.Consumer
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 02/05/2019.
 */
class UpdateFileChecker : JobService() {
    private var repository: SubmittedUpdateFileRepository? = null

    override fun onStartJob(params: JobParameters): Boolean {
        return try {
            performFileCheck(params)
        } catch (e: Exception) {
            logError(TAG, "Error in scheduled update file name check", e)
            jobFinished(params, false)
            true // Do not show any failures / crashes to the user - this is a background process.
        }

    }

    override fun onStopJob(params: JobParameters): Boolean {
        if (repository != null) {
            repository!!.close()
            repository = null
        }
        return true
    }

    private fun performFileCheck(params: JobParameters): Boolean {
        logDebug(TAG, "Started update file check")

        if (!Utils.checkNetworkConnection(applicationContext)) {
            logDebug(TAG, "Network unavailable, skipping update file check")
            jobFinished(params, false)
            return true // do not perform any action without network.
        }

        val openNetworkCalls = AtomicInteger(0)
        repository = SubmittedUpdateFileRepository(applicationContext)

        for (directoryName in UPDATE_DIRECTORIES) {
            val directory = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT), directoryName)

            if (!directory.exists()) {
                logDebug(TAG, "Directory: " + directory.absolutePath + " does not exist. Skipping...")
                continue
            }

            logDebug(TAG, "Started checking for update files in directory: " + directory.absolutePath)

            val fileNamesInDirectory = ArrayList<String>()
            getAllFileNames(directory, fileNamesInDirectory)

            for (fileName in fileNamesInDirectory) {
                logDebug(TAG, "Found update file: $fileName")
                if (!repository!!.isFileAlreadySubmitted(fileName)) {

                    val application = application
                    val callback = Consumer<ServerPostResult> { serverPostResult ->
                        if (serverPostResult == null) {
                            // network error, try again later
                            logWarning(TAG, NetworkException("Error submitting update file $fileName: No network connection or empty response"))
                        } else if (!serverPostResult.isSuccess) {
                            val errorMessage = serverPostResult.errorMessage
                            // If file is already in our database or if file is an invalid temporary file (server decides when this is the case),
                            // mark this file as submitted but don't inform the user about it.
                            if (errorMessage != null && (errorMessage == FILE_ALREADY_IN_DATABASE || errorMessage == FILENAME_INVALID)) {
                                logInfo(TAG, "Ignoring submitted update file $fileName, already in database or not relevant")
                                repository!!.store(fileName)

                                // Log failed contribution
                                val analyticsBundle = Bundle()
                                analyticsBundle.putString("CONTRIBUTION_FILENAME", fileName)
                                FirebaseAnalytics.getInstance(application).logEvent("CONTRIBUTION_NOT_NEEDED", analyticsBundle)
                            } else {
                                // server error, try again later
                                logError(TAG, NetworkException("Error submitting update file " + fileName + ": " + serverPostResult.errorMessage))
                            }
                        } else {
                            logInfo(TAG, "Successfully submitted update file $fileName")
                            // Inform user of successful contribution (only if the file is not a "bogus" temporary file)
                            if (fileName.contains(".zip")) {
                                LocalNotifications.showContributionSuccessfulNotification(application, fileName)
                                // Increase number of submitted updates. Not currently shown in the UI, but may come in handy later.
                                val settingsManager = SettingsManager(application)
                                settingsManager.savePreference(
                                        SettingsManager.PROPERTY_CONTRIBUTION_COUNT,
                                        settingsManager.getPreference(SettingsManager.PROPERTY_CONTRIBUTION_COUNT, 0) + 1
                                )

                                // Log successful contribution
                                val analyticsBundle = Bundle()
                                analyticsBundle.putString("CONTRIBUTION_FILENAME", fileName)
                                FirebaseAnalytics.getInstance(application).logEvent("CONTRIBUTION_SUCCESSFUL", analyticsBundle)
                            }

                            // Store the filename in a local database to prevent re-submission until it gets installed or removed by the user.
                            repository!!.store(fileName)
                        }

                        if (openNetworkCalls.decrementAndGet() <= 0) {
                            jobFinished(params, false)
                        }
                    }
                    logDebug(TAG, "Submitting update file $fileName")
                    openNetworkCalls.incrementAndGet()
                    (application as ApplicationData).getServerConnector().submitUpdateFile(fileName, callback)
                } else {
                    logDebug(TAG, "Update file $fileName has already been submitted. Ignoring...")
                }
            }

            logDebug(TAG, "Finished checking for update files in directory: " + directory.absolutePath)
        }

        if (openNetworkCalls.get() <= 0) {
            jobFinished(params, false)
        }
        return true
    }

    private fun getAllFileNames(folder: File, result: MutableList<String>) {
        val files = folder.listFiles() ?: return

        // Happens when the user has revoked the "read external storage" permission of the app

        for (f in files) {

            if (f.isDirectory) {
                getAllFileNames(f, result)
            }

            if (f.isFile) {
                result.add(f.name)
            }
        }
    }

    companion object {
        private val UPDATE_DIRECTORIES = arrayOf(".Ota")
        private const val TAG = "UpdateFileChecker"
        private const val FILE_ALREADY_IN_DATABASE = "E_FILE_ALREADY_IN_DB"
        private const val FILENAME_INVALID = "E_FILE_INVALID"
    }
}
