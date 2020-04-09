package com.arjanvlek.oxygenupdater.utils

import com.arjanvlek.oxygenupdater.BuildConfig.NOTIFICATIONS_PREFIX
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager.Companion.PROPERTY_NOTIFICATION_TOPIC
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.UpdateMethod
import com.arjanvlek.oxygenupdater.utils.Logger.logVerbose
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.java.KoinJavaComponent.inject

object NotificationTopicSubscriber {

    private const val TAG = "NotificationTopicSubscriber"
    private const val DEVICE_TOPIC_PREFIX = "device_"
    private const val UPDATE_METHOD_TOPIC_PREFIX = "_update-method_"

    private val settingsManager by inject(SettingsManager::class.java)

    fun subscribe(deviceList: List<Device>, updateMethodList: List<UpdateMethod>) {
        val oldTopic = settingsManager.getPreference<String?>(PROPERTY_NOTIFICATION_TOPIC, null)

        if (oldTopic == null) {
            // If the topic is not saved (App Version 1.0.0 did not do this),
            // unsubscribe from all possible topics first to prevent duplicate/wrong notifications.
            updateMethodList.forEach { (id) ->
                deviceList.forEach { device ->
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(
                        NOTIFICATIONS_PREFIX + DEVICE_TOPIC_PREFIX + device.id + UPDATE_METHOD_TOPIC_PREFIX + id
                    )
                }
            }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(oldTopic)
        }

        val newTopic = (NOTIFICATIONS_PREFIX + DEVICE_TOPIC_PREFIX
                + settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
                + UPDATE_METHOD_TOPIC_PREFIX
                + settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L))

        // Subscribe to the new topic to start receiving notifications.
        FirebaseMessaging.getInstance().subscribeToTopic(newTopic)

        logVerbose(TAG, "Subscribed to notifications on topic $newTopic ...")

        settingsManager.savePreference(PROPERTY_NOTIFICATION_TOPIC, newTopic)
    }
}
