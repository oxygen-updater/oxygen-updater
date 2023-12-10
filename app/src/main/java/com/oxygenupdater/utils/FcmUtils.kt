package com.oxygenupdater.utils

import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessaging
import com.oxygenupdater.BuildConfig.NOTIFICATIONS_PREFIX
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.set
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyDeviceId
import com.oxygenupdater.internal.settings.KeyNotificationTopic
import com.oxygenupdater.internal.settings.KeyUpdateMethodId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmUtils @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val firebaseMessaging: FirebaseMessaging,
) {

    private val currentTopic: String
        get() {
            val deviceId = sharedPreferences[KeyDeviceId, NotSetL]
            val updateMethodId = sharedPreferences[KeyUpdateMethodId, NotSetL]

            return DeviceTopicPrefix + deviceId + UpdateMethodTopicPrefix + updateMethodId
        }

    /**
     * 1. Unsubscribe from old topic (if any, and if different from [currentTopic])
     *    to avoid duplicate/incorrect notifications.
     * 2. Subscribe to new/current [currentTopic]. This is never skipped, because
     *    FCM registration token might've changed and we should account for it..
     */
    fun resubscribe() {
        val oldTopic = sharedPreferences[KeyNotificationTopic, ""]
        val currentTopic = currentTopic

        // 1. Unsubscribe from old topic (if changed)
        if (oldTopic.isNotEmpty() && oldTopic != currentTopic) {
            firebaseMessaging.unsubscribeFromTopic(oldTopic)
            logDebug(TAG, "Unsubscribed from old topic: $oldTopic")
        }

        // 2. Subscribe to new/current topic
        firebaseMessaging.subscribeToTopic(currentTopic)
        logDebug(TAG, "Subscribed to new topic: $currentTopic")
        sharedPreferences[KeyNotificationTopic] = currentTopic
    }

    companion object {
        private const val TAG = "FcmUtils"
        private const val DeviceTopicPrefix = "${NOTIFICATIONS_PREFIX}device_"
        private const val UpdateMethodTopicPrefix = "_update-method_"
    }
}
