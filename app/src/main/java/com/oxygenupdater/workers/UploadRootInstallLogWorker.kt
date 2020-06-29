package com.oxygenupdater.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oxygenupdater.exceptions.NetworkException
import com.oxygenupdater.exceptions.OxygenUpdaterException
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.InstallationStatus
import com.oxygenupdater.models.RootInstall
import com.oxygenupdater.models.ServerPostResult
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Utils.SERVER_TIME_ZONE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import org.threeten.bp.LocalDateTime

/**
 * Uploads root installation logs, after an installation has completed, to the backend
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class UploadRootInstallLogWorker(
    context: Context,
    private val parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val serverRepository by inject(ServerRepository::class.java)
    private val settingsManager by inject(SettingsManager::class.java)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        val timestamp = LocalDateTime.now(SERVER_TIME_ZONE).toString()

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
            status = InstallationStatus.valueOf(getString(WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_STATUS) ?: "")
            installationId = getString(WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_INSTALL_ID) ?: "<INVALID>"
            startOSVersion = getString(WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_START_OS) ?: "<UNKNOWN>"
            destinationOSVersion = getString(WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_DESTINATION_OS) ?: "<UNKNOWN>"
            currentOsVersion = getString(WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_CURR_OS) ?: "<UNKNOWN>"
            failureReason = getString(WORK_DATA_UPLOAD_ROOT_INSTALL_LOG_FAILURE_REASON) ?: ""
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
        private const val TAG = "UploadRootInstallLogWorker"
    }
}
