package com.oxygenupdater.services

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.oxygenupdater.enums.NotificationElement
import com.oxygenupdater.enums.NotificationType
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.utils.logDebug
import com.oxygenupdater.utils.logError
import com.oxygenupdater.workers.DisplayDelayedNotificationWorker
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class FirebaseMessagingService : FirebaseMessagingService() {

    private val random = Random.Default

    private val workManager by inject<WorkManager>()

    override fun onNewToken(token: String) {
        logDebug(TAG, "Received new Firebase token: $token")
        PrefManager.putString(PrefManager.KeyFirebaseToken, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) = try {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DisplayDelayedNotificationWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().apply {
                remoteMessage.data.forEach { (key, value) ->
                    putString(key, value)
                }
            }.build())

        // Add a random delay to avoid overloading the server, but only if this is not a new version notification
        try {
            NotificationType.valueOf(
                remoteMessage.data[NotificationElement.TYPE.name] ?: ""
            ) == NotificationType.NEW_VERSION
        } catch (e: IllegalArgumentException) {
            false
        }.let {
            if (!it) return@let // new version notifications should be shown immediately

            val serverSpecifiedDelay = PrefManager.getInt(PrefManager.KeyNotificationDelayInSeconds, 10)
            val delay = random.nextLong(1, serverSpecifiedDelay.coerceAtLeast(2).toLong())
            logDebug(TAG, "Displaying push notification in $delay second(s)")
            oneTimeWorkRequest.setInitialDelay(delay, TimeUnit.SECONDS)
        }

        workManager.enqueue(oneTimeWorkRequest.build()).let {}
    } catch (e: Exception) {
        logError(TAG, "Error dispatching push notification", e)
    }

    companion object {
        private const val TAG = "FirebaseMessagingService"
    }
}
