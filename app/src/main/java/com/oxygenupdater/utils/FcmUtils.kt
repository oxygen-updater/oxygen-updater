package com.oxygenupdater.utils

import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessaging
import com.oxygenupdater.BuildConfig.NOTIFICATIONS_PREFIX
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.set
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyDeviceId
import com.oxygenupdater.internal.settings.KeyNotificationDeviceTopic
import com.oxygenupdater.internal.settings.KeyNotificationFullTopic
import com.oxygenupdater.internal.settings.KeyUpdateMethodId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmUtils @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val firebaseMessaging: FirebaseMessaging,
) {

    /**
     * Added in v6.2.0 to make it easier & quicker to send notifications to devices
     * regardless of the selected update method.
     *
     * However, this should be used only once enough time passes until most people
     * have updated to this version.
     */
    private val currentDeviceTopic: String
        get() = DeviceTopicPrefix + sharedPreferences[KeyDeviceId, NotSetL]

    private val currentFullTopic: String
        get() = currentDeviceTopic + UpdateMethodTopicPrefix + sharedPreferences[KeyUpdateMethodId, NotSetL]

    fun resubscribe() {
        resubscribe(key = KeyNotificationDeviceTopic, currentTopic = currentDeviceTopic)
        resubscribe(key = KeyNotificationFullTopic, currentTopic = currentFullTopic)
    }

    /**
     * 1. Unsubscribe from old topic (if any, and if different from current topics)
     *    to avoid duplicate/incorrect notifications.
     * 2. Subscribe to new/current topics. This is never skipped, because FCM
     *    registration token might've changed and we should account for it.
     */
    private fun resubscribe(key: String, currentTopic: String) {
        // 1. Unsubscribe from old topic (if changed)
        val oldTopic = sharedPreferences[key, ""]
        if (oldTopic.isNotEmpty() && oldTopic != currentTopic) {
            firebaseMessaging.unsubscribeFromTopic(oldTopic)
            logDebug(TAG, "Unsubscribed from old topic: $oldTopic")
        }

        // 2. Subscribe to new/current topic
        firebaseMessaging.subscribeToTopic(currentTopic)
        logDebug(TAG, "Subscribed to new topic: $currentTopic")
        sharedPreferences[key] = currentTopic
    }

    companion object {
        private const val TAG = "FcmUtils"
        private const val DeviceTopicPrefix = "${NOTIFICATIONS_PREFIX}device_"
        private const val UpdateMethodTopicPrefix = "_update-method_"
    }
}
