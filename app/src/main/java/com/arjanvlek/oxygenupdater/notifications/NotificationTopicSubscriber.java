package com.arjanvlek.oxygenupdater.notifications;

import android.os.AsyncTask;
import android.util.Log;

import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.UpdateMethod;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;

import static com.arjanvlek.oxygenupdater.ApplicationContext.DEVICE_TOPIC_PREFIX;
import static com.arjanvlek.oxygenupdater.ApplicationContext.UPDATE_METHOD_TOPIC_PREFIX;
import static com.arjanvlek.oxygenupdater.BuildConfig.NOTIFICATIONS_PREFIX;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_NOTIFICATION_TOPIC;

public class NotificationTopicSubscriber extends AsyncTask<TopicSubscriptionData, Integer, TopicSubscriptionData> {

    private static final String TAG = "NotificationTopicSubscr";

    @Override
    public TopicSubscriptionData doInBackground(TopicSubscriptionData... datas) {
        TopicSubscriptionData data = datas[0];

        SettingsManager settingsManager = new SettingsManager(data.getApplicationContext().getApplicationContext());

        String oldTopic = settingsManager.getPreference(PROPERTY_NOTIFICATION_TOPIC);

        if(oldTopic == null) {
            data.setDevices(data.getApplicationContext().getServerConnector().getDevices());
            data.setUpdateMethods(data.getApplicationContext().getServerConnector().getAllUpdateMethods());
        }
        if (data.getDevices() == null) data.setDevices(new ArrayList<>());
        if (data.getUpdateMethods() == null) data.setUpdateMethods(new ArrayList<>());
        return data;
    }

    @Override
    public void onPostExecute(TopicSubscriptionData data) {
        SettingsManager settingsManager = new SettingsManager(data.getApplicationContext().getApplicationContext());

        String oldTopic = settingsManager.getPreference(PROPERTY_NOTIFICATION_TOPIC);

        if (oldTopic == null) {
            // If the topic is not saved (Version 1.0.0 did not do this), unsubscribe from all possible topics first to prevent duplicate / wrong notifications.
            for(UpdateMethod method : data.getUpdateMethods()) {
                for(Device device : data.getDevices()) {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATIONS_PREFIX + DEVICE_TOPIC_PREFIX + device.getId() + UPDATE_METHOD_TOPIC_PREFIX + method.getId());
                }
            }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(oldTopic);
        }

        String newTopic = NOTIFICATIONS_PREFIX + DEVICE_TOPIC_PREFIX + data.getDeviceId() + UPDATE_METHOD_TOPIC_PREFIX + data.getUpdateMethodId();

        // Subscribe to the new topic to start receiving notifications.
        FirebaseMessaging.getInstance().subscribeToTopic(newTopic);
        Log.v(TAG, "Subscribed to notifications on topic " + newTopic + " ...");
        settingsManager.savePreference(PROPERTY_NOTIFICATION_TOPIC, newTopic);
    }
}
