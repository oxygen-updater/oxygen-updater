package com.arjanvlek.oxygenupdater.services

import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.utils.Logger.logDebug
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.Utils
import com.arjanvlek.oxygenupdater.workers.DisplayDelayedNotificationWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class FirebaseMessagingService : FirebaseMessagingService() {

    private val workManager by inject<WorkManager>()
    private val settingsManager by inject<SettingsManager>()

    override fun onNewToken(token: String) {
        logDebug(TAG, "Received new Firebase token: $token")
        settingsManager.savePreference(SettingsManager.PROPERTY_FIREBASE_TOKEN, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            // Receive the notification contents but build/show the actual notification
            // with a small random delay to avoid overloading the server.
            val displayDelayInSeconds = Utils.randomBetween(
                1,
                settingsManager.getPreference(SettingsManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, 1800)
            )

            logDebug(TAG, "Displaying push notification in $displayDelayInSeconds second(s)")

            val pairs = remoteMessage.data.map {
                Pair(it.key, it.value)
            }.toTypedArray()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DisplayDelayedNotificationWorker>()
                .setInputData(workDataOf(*pairs))
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueue(oneTimeWorkRequest)
        } catch (e: Exception) {
            logError(TAG, "Error dispatching push notification", e)
        }
    }

    companion object {
        const val TAG = "FirebaseMessagingService"
    }
}
