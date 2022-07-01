package com.oxygenupdater.services

import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.oxygenupdater.enums.NotificationElement
import com.oxygenupdater.enums.NotificationType
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.workers.DisplayDelayedNotificationWorker
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class FirebaseMessagingService : FirebaseMessagingService() {

    private val random = Random.Default

    private val workManager by inject<WorkManager>()

    override fun onNewToken(token: String) {
        logDebug(TAG, "Received new Firebase token: $token")
        PrefManager.putString(PrefManager.PROPERTY_FIREBASE_TOKEN, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            val serverSpecifiedDelay = PrefManager.getInt(
                PrefManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS,
                300
            ).let {
                val isNewVersionNotification = NotificationType.valueOf(
                    remoteMessage.data[NotificationElement.TYPE.name] ?: ""
                ) == NotificationType.NEW_VERSION

                // The app should notify people about new versions ASAP, so bypass the server-specified
                // delay if the notification is a "system update available" one.
                // Also, `random.nextLong(from, until)` throws an exception if `until` and `from` have
                // the same value. So make sure they're never the same.
                if (it == 1 || isNewVersionNotification) 2 else it
            }

            // Receive the notification contents but build/show the actual notification
            // with a small random delay to avoid overloading the server.
            val displayDelayInSeconds = random.nextLong(
                1,
                serverSpecifiedDelay.toLong()
            )

            logDebug(TAG, "Displaying push notification in $displayDelayInSeconds second(s)")

            val pairs = remoteMessage.data.map {
                Pair(it.key, it.value)
            }.toTypedArray()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DisplayDelayedNotificationWorker>()
                .setInitialDelay(displayDelayInSeconds, TimeUnit.SECONDS)
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
