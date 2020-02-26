package com.arjanvlek.oxygenupdater.services

import android.app.job.JobParameters
import android.app.job.JobService
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.exceptions.NetworkException
import com.arjanvlek.oxygenupdater.exceptions.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.InstallationStatus
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.koin.android.ext.android.inject

class RootInstallLogger : JobService() {

    private val settingsManager by inject<SettingsManager>()

    override fun onStartJob(params: JobParameters?): Boolean {
        if (params == null || application !is OxygenUpdater) {
            // Retrying wont fix this issue. This is a lost cause.
            return true
        }

        val applicationData = application as OxygenUpdater
        val serverConnector = applicationData.serverConnector
        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        if (deviceId == -1L || updateMethodId == -1L) {
            logError(
                TAG,
                OxygenUpdaterException("Failed to log update installation action: Device and / or update method not selected.")
            )

            // Retrying wont fix this issue. This is a lost cause.
            return true
        }

        params.extras.apply {
            val status = InstallationStatus.valueOf(getString(DATA_STATUS, ""))
            val installationId = getString(DATA_INSTALL_ID, "<INVALID>")
            val startOSVersion = getString(DATA_START_OS, "<UNKNOWN>")
            val destinationOSVersion = getString(DATA_DESTINATION_OS, "<UNKNOWN>")
            val currentOsVersion = getString(DATA_CURR_OS, "<UNKNOWN>")
            val timestamp = LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")).toString()
            val failureReason = getString(DATA_FAILURE_REASON, "")

            val installation = RootInstall(deviceId, updateMethodId, status, installationId, timestamp, startOSVersion, destinationOSVersion, currentOsVersion, failureReason)

            serverConnector!!.logRootInstall(installation) { result: ServerPostResult? ->
                if (result == null) {
                    logError(
                        TAG,
                        NetworkException("Failed to log update installation action on server: No response from server")
                    )
                    jobFinished(params, true)
                } else if (!result.success) {
                    logError(
                        TAG,
                        OxygenUpdaterException("Failed to log update installation action on server: " + result.errorMessage)
                    )
                    jobFinished(params, true)
                } else if (result.success
                    && installation.installationStatus == InstallationStatus.FAILED
                    || installation.installationStatus == InstallationStatus.FINISHED
                ) {
                    settingsManager.deletePreference(SettingsManager.PROPERTY_INSTALLATION_ID)
                    jobFinished(params, false)
                } else {
                    jobFinished(params, false)
                }
            }
        }

        return true
    }

    override fun onStopJob(params: JobParameters) = true

    companion object {
        private const val TAG = "RootInstallLogger"

        const val DATA_STATUS = "STATUS"
        const val DATA_INSTALL_ID = "INSTALLATION_ID"
        const val DATA_START_OS = "START_OS"
        const val DATA_DESTINATION_OS = "DEST_OS"
        const val DATA_CURR_OS = "CURR_OS"
        const val DATA_FAILURE_REASON = "FAILURE_REASON"
    }
}
