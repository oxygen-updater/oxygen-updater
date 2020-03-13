package com.arjanvlek.oxygenupdater.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arjanvlek.oxygenupdater.exceptions.NetworkException
import com.arjanvlek.oxygenupdater.exceptions.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.InstallationStatus
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.koin.java.KoinJavaComponent.inject

/**
 * Uploads root installation logs, after an installation has completed, to the backend
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class RootInstallLogUploadWorker(
    context: Context,
    private val parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val serverRepository by inject(ServerRepository::class.java)
    private val settingsManager by inject(SettingsManager::class.java)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        val timestamp: String = LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")).toString()

        if (deviceId == -1L || updateMethodId == -1L) {
            logError(
                TAG,
                OxygenUpdaterException("Failed to log update installation action: Device and / or update method not selected.")
            )

            // Retrying wont fix this issue. This is a lost cause.
            Result.failure()
        }

        var status: InstallationStatus
        var installationId: String
        var startOSVersion: String
        var destinationOSVersion: String
        var currentOsVersion: String
        var failureReason: String

        parameters.inputData.run {
            status = InstallationStatus.valueOf(getString(WORK_DATA_LOG_UPLOAD_STATUS) ?: "")
            installationId = getString(WORK_DATA_LOG_UPLOAD_INSTALL_ID) ?: "<INVALID>"
            startOSVersion = getString(WORK_DATA_LOG_UPLOAD_START_OS) ?: "<UNKNOWN>"
            destinationOSVersion = getString(WORK_DATA_LOG_UPLOAD_DESTINATION_OS) ?: "<UNKNOWN>"
            currentOsVersion = getString(WORK_DATA_LOG_UPLOAD_CURR_OS) ?: "<UNKNOWN>"
            failureReason = getString(WORK_DATA_LOG_UPLOAD_FAILURE_REASON) ?: ""
        }

        val installation = RootInstall(
            deviceId,
            updateMethodId,
            status,
            installationId,
            timestamp,
            startOSVersion,
            destinationOSVersion,
            currentOsVersion,
            failureReason
        )
        val result: ServerPostResult? = serverRepository.logRootInstall(installation)
        if (result == null) {
            logError(
                TAG,
                NetworkException("Failed to log update installation action on server: No response from server")
            )
            Result.retry()
        } else if (!result.success) {
            logError(
                TAG,
                OxygenUpdaterException("Failed to log update installation action on server: " + result.errorMessage)
            )
            Result.retry()
        } else if (result.success
            && installation.installationStatus == InstallationStatus.FAILED
            || installation.installationStatus == InstallationStatus.FINISHED
        ) {
            settingsManager.deletePreference(SettingsManager.PROPERTY_INSTALLATION_ID)
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "RootInstallLogUploadWorker"
    }
}
