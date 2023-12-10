package com.oxygenupdater.services

import android.content.SharedPreferences
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.oxygenupdater.enums.NotificationElement
import com.oxygenupdater.enums.NotificationType
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.set
import com.oxygenupdater.internal.settings.KeyFirebaseToken
import com.oxygenupdater.internal.settings.KeyNotificationDelayInSeconds
import com.oxygenupdater.utils.FcmUtils
import com.oxygenupdater.utils.logDebug
import com.oxygenupdater.utils.logError
import com.oxygenupdater.workers.DisplayDelayedNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var fcmUtils: FcmUtils

    @Inject
    lateinit var crashlytics: FirebaseCrashlytics

    override fun onNewToken(token: String) {
        logDebug(TAG, "Received new Firebase token: $token")
        sharedPreferences[KeyFirebaseToken] = token
        fcmUtils.resubscribe()
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

            val serverSpecifiedDelay = sharedPreferences[KeyNotificationDelayInSeconds, 10]
            val delay = Random.nextLong(1, serverSpecifiedDelay.coerceAtLeast(2).toLong())
            logDebug(TAG, "Displaying push notification in $delay second(s)")
            oneTimeWorkRequest.setInitialDelay(delay, TimeUnit.SECONDS)
        }

        workManager.enqueue(oneTimeWorkRequest.build()).let {}
    } catch (e: Exception) {
        crashlytics.logError(TAG, "Error dispatching push notification", e)
    }

    companion object {
        private const val TAG = "FirebaseMessagingService"
    }
}
