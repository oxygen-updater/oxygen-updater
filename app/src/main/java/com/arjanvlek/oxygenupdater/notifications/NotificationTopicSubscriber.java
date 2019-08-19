package com.arjanvlek.oxygenupdater.notifications;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.UpdateMethod;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.google.firebase.messaging.FirebaseMessaging;

import static com.arjanvlek.oxygenupdater.ApplicationData.DEVICE_TOPIC_PREFIX;
import static com.arjanvlek.oxygenupdater.ApplicationData.UPDATE_METHOD_TOPIC_PREFIX;
import static com.arjanvlek.oxygenupdater.BuildConfig.NOTIFICATIONS_PREFIX;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_NOTIFICATION_TOPIC;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

public class NotificationTopicSubscriber {

	private static final String TAG = "NotificationTopicSubscriber";

	public static void subscribe(ApplicationData data) {
		SettingsManager settingsManager = new SettingsManager(data.getApplicationContext());
		ServerConnector serverConnector = data.getServerConnector();

		String oldTopic = settingsManager.getPreference(PROPERTY_NOTIFICATION_TOPIC, null);

		if (oldTopic == null) {
			serverConnector.getDevices(devices -> serverConnector.getAllUpdateMethods(updateMethods -> {
				// If the topic is not saved (App Version 1.0.0 did not do this), unsubscribe from all possible topics first to prevent duplicate / wrong notifications.
				for (UpdateMethod method : updateMethods) {
					for (Device device : devices) {
						FirebaseMessaging.getInstance()
								.unsubscribeFromTopic(NOTIFICATIONS_PREFIX + DEVICE_TOPIC_PREFIX + device.getId() + UPDATE_METHOD_TOPIC_PREFIX + method.getId());
					}
				}
			}));
		} else {
			FirebaseMessaging.getInstance().unsubscribeFromTopic(oldTopic);
		}

		String newTopic = NOTIFICATIONS_PREFIX + DEVICE_TOPIC_PREFIX
				+ settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L)
				+ UPDATE_METHOD_TOPIC_PREFIX
				+ settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L);

		// Subscribe to the new topic to start receiving notifications.
		FirebaseMessaging.getInstance().subscribeToTopic(newTopic);
		logVerbose(TAG, "Subscribed to notifications on topic " + newTopic + " ...");
		settingsManager.savePreference(PROPERTY_NOTIFICATION_TOPIC, newTopic);

	}
}
