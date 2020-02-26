package com.arjanvlek.oxygenupdater.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_STATUS
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.os.persistableBundleOf
import com.arjanvlek.oxygenupdater.OxygenUpdater.Companion.NO_OXYGEN_OS
import com.arjanvlek.oxygenupdater.OxygenUpdater.Companion.PUSH_NOTIFICATION_CHANNEL_ID
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.activities.MainActivity
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager.Companion.PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT
import com.arjanvlek.oxygenupdater.models.InstallationStatus
import com.arjanvlek.oxygenupdater.models.SystemVersionProperties
import com.arjanvlek.oxygenupdater.services.RootInstallLogger
import com.arjanvlek.oxygenupdater.services.RootInstallLogger.Companion.DATA_FAILURE_REASON
import com.arjanvlek.oxygenupdater.services.RootInstallLogger.Companion.DATA_STATUS
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.Utils
import org.koin.java.KoinJavaComponent.inject

class VerifyInstallationReceiver : BroadcastReceiver() {

    private val settingsManager by inject(SettingsManager::class.java)

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (settingsManager.getPreference(PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, false)
                && intent.action != null && (intent.action == "android.intent.action.BOOT_COMPLETED")
            ) {
                settingsManager.savePreference(PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT, false)

                val properties = SystemVersionProperties()

                // Don't check on unsupported devices.
                if ((properties.oxygenOSVersion == NO_OXYGEN_OS) || (properties.oxygenOSOTAVersion == NO_OXYGEN_OS)) {
                    return
                }

                val oldOxygenOSVersion = settingsManager.getPreference(SettingsManager.PROPERTY_OLD_SYSTEM_VERSION, "")
                val targetOxygenOSVersion = settingsManager.getPreference(SettingsManager.PROPERTY_TARGET_SYSTEM_VERSION, "")
                val currentOxygenOSVersion = properties.oxygenOSOTAVersion

                if (oldOxygenOSVersion.isEmpty() || targetOxygenOSVersion.isEmpty() || currentOxygenOSVersion.isEmpty()) {
                    displayFailureNotification(context, context.getString(R.string.install_verify_error_unable_to_verify))
                    logFailure(context, oldOxygenOSVersion, targetOxygenOSVersion, currentOxygenOSVersion, "ERR_CHECK_FAILED")
                } else if (currentOxygenOSVersion == oldOxygenOSVersion) {
                    displayFailureNotification(context, context.getString(R.string.install_verify_error_nothing_installed))
                    logFailure(context, oldOxygenOSVersion, targetOxygenOSVersion, currentOxygenOSVersion, "ERR_INSTALL_FAILED")
                } else if (currentOxygenOSVersion != targetOxygenOSVersion) {
                    displayFailureNotification(context, context.getString(R.string.install_verify_error_wrong_version_installed))
                    logFailure(context, oldOxygenOSVersion, targetOxygenOSVersion, currentOxygenOSVersion, "ERR_WRONG_OS_INSTALLED")
                } else {
                    displaySuccessNotification(context, properties.oxygenOSVersion)
                    logSuccess(context, oldOxygenOSVersion, targetOxygenOSVersion, currentOxygenOSVersion)
                }
            }
        } catch (e: Throwable) {
            logError(TAG, "Failed to check if update was successfully installed", e)
        }
    }

    private fun displaySuccessNotification(context: Context, oxygenOSVersion: String) {
        val contentIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), 0)

        val builder = NotificationCompat.Builder(context, PUSH_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.done_circle)
            .setOngoing(false)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(PRIORITY_HIGH)
            .setContentTitle(context.getString(R.string.install_verify_success_title))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.install_verify_success_message, oxygenOSVersion)))
            .setContentText(context.getString(R.string.install_verify_success_message, oxygenOSVersion))
            .setCategory(CATEGORY_STATUS)

        (Utils.getSystemService(context, NOTIFICATION_SERVICE) as NotificationManager).apply {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun displayFailureNotification(context: Context, errorMessage: String) {
        val contentIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), 0)

        val builder = NotificationCompat.Builder(context, PUSH_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.error_outline)
            .setOngoing(false)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .setContentTitle(context.getString(R.string.install_verify_error_title))
            .setContentText(errorMessage)
            .setCategory(CATEGORY_STATUS)

        (Utils.getSystemService(context, NOTIFICATION_SERVICE) as NotificationManager).apply {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun logSuccess(
        context: Context,
        startOs: String,
        destinationOs: String,
        currentOs: String
    ) = buildLogData(startOs, destinationOs, currentOs).let {
        it.putString(DATA_STATUS, InstallationStatus.FINISHED.toString())

        scheduleLogUploadTask(context, it)
    }

    private fun logFailure(
        context: Context,
        startOs: String,
        destinationOs: String,
        currentOs: String,
        reason: String
    ) = buildLogData(startOs, destinationOs, currentOs).let {
        it.putString(DATA_STATUS, InstallationStatus.FAILED.toString())
        it.putString(DATA_FAILURE_REASON, reason)

        scheduleLogUploadTask(context, it)
    }

    private fun buildLogData(startOs: String, destinationOs: String, currentOs: String) = persistableBundleOf(
        RootInstallLogger.DATA_INSTALL_ID to settingsManager.getPreference(SettingsManager.PROPERTY_INSTALLATION_ID, "<INVALID>"),
        RootInstallLogger.DATA_START_OS to startOs,
        RootInstallLogger.DATA_DESTINATION_OS to destinationOs,
        RootInstallLogger.DATA_CURR_OS to currentOs
    )

    private fun scheduleLogUploadTask(context: Context, logData: PersistableBundle) {
        val task = JobInfo.Builder(TASK_ID, ComponentName(context, RootInstallLogger::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .setMinimumLatency(3000)
            .setExtras(logData)
            .setBackoffCriteria(3000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)

        if (Build.VERSION.SDK_INT >= 26) {
            task.setRequiresBatteryNotLow(false)
            task.setRequiresStorageNotLow(false)
        }

        (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).apply {
            schedule(task.build())
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 79243095
        private const val TASK_ID = 395819383
        private const val TAG = "VerifyInstallReceiver"
    }
}
