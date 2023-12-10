package com.oxygenupdater.utils

import com.google.firebase.messaging.FirebaseMessaging
import com.oxygenupdater.BuildConfig.NOTIFICATIONS_PREFIX
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.KeyDeviceId
import com.oxygenupdater.internal.settings.PrefManager.KeyNotificationTopic
import com.oxygenupdater.internal.settings.PrefManager.KeyUpdateMethodId

object FcmUtils {

    private const val TAG = "FcmUtils"
    private const val DeviceTopicPrefix = "${NOTIFICATIONS_PREFIX}device_"
    private const val UpdateMethodTopicPrefix = "_update-method_"

    private val currentTopic: String
        get() {
            val deviceId = PrefManager.getLong(KeyDeviceId, NotSetL)
            val updateMethodId = PrefManager.getLong(KeyUpdateMethodId, NotSetL)

            return DeviceTopicPrefix + deviceId + UpdateMethodTopicPrefix + updateMethodId
        }

    private val firebaseMessaging by lazy(LazyThreadSafetyMode.NONE) {
        FirebaseMessaging.getInstance()
    }

    /**
     * 1. Unsubscribe from old topic (if any, and if different from [currentTopic])
     *    to avoid duplicate/incorrect notifications.
     * 2. Subscribe to new/current [currentTopic]. This is never skipped, because
     *    FCM registration token might've changed and we should account for it.
     */
    fun resubscribe() {
        val oldTopic = PrefManager.getString(KeyNotificationTopic, null)
        val currentTopic = currentTopic

        // 1. Unsubscribe from old topic (if changed)
        if (!oldTopic.isNullOrEmpty() && oldTopic != currentTopic) {
            firebaseMessaging.unsubscribeFromTopic(oldTopic)
            logDebug(TAG, "Unsubscribed from old topic: $oldTopic")
        }

        // 2. Subscribe to new/current topic
        firebaseMessaging.subscribeToTopic(currentTopic)
        logDebug(TAG, "Subscribed to new topic: $currentTopic")
        PrefManager.putString(KeyNotificationTopic, currentTopic)
    }
}
