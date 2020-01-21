package com.arjanvlek.oxygenupdater.notifications

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.objectMapper
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            val settingsManager = SettingsManager(applicationContext)

            //  Receive the notification contents but build / show the actual notification with a small random delay to avoid overloading the server.
            val messageContents = remoteMessage.data
            val displayDelayInSeconds = Utils.randomBetween(1, settingsManager.getPreference(SettingsManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, 1800))

            logDebug(TAG, "Displaying push notification in $displayDelayInSeconds second(s)")

            val taskData = PersistableBundle()
            val scheduler: JobScheduler? = application.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            if (scheduler == null) {
                logError(TAG, OxygenUpdaterException("Job scheduler service is not available"))
                return
            }

            val jobId = AVAILABLE_JOB_IDS
                .find { id -> scheduler.allPendingJobs.none { it.id == id } }
                ?: throw RuntimeException("There are too many notifications scheduled. Cannot schedule a new notification!")

            taskData.putString(
                DelayedPushNotificationDisplayer.KEY_NOTIFICATION_CONTENTS, objectMapper.writeValueAsString(messageContents)
            )

            val task = JobInfo.Builder(jobId, ComponentName(application, DelayedPushNotificationDisplayer::class.java))
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setMinimumLatency(displayDelayInSeconds * 1000.toLong())
                .setExtras(taskData)

            if (Build.VERSION.SDK_INT >= 26) {
                task.setRequiresBatteryNotLow(false)
                task.setRequiresStorageNotLow(false)
            }

            scheduler.schedule(task.build())
        } catch (e: Exception) {
            logError(TAG, "Error dispatching push notification", e)
        }
    }

    companion object {
        const val TAG = "NotificationService"
        private val AVAILABLE_JOB_IDS = listOf(8326, 8327, 8328, 8329, 8330, 8331, 8332, 8333)
    }
}
