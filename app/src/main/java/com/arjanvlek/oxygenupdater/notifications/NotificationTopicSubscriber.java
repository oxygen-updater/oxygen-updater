package com.arjanvlek.oxygenupdater.notifications;

import android.os.AsyncTask;

import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.UpdateMethod;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;

public class NotificationTopicSubscriber extends AsyncTask<TopicSubscriptionData, Integer, TopicSubscriptionData> {

    private List<Device> devices;
    private List<UpdateMethod> updateMethods;

    @Override
    public TopicSubscriptionData doInBackground(TopicSubscriptionData... datas) {
        TopicSubscriptionData data = datas[0];
        this.devices = data.getApplicationContext().getDevices();
        this.updateMethods = data.getApplicationContext().getServerConnector().getAllUpdateMethods();
        return data;
    }

    @Override
    public void onPostExecute(TopicSubscriptionData data) {
        // Unsubscribe from all possible topics first to prevent duplicate / wrong notifications.
        for(UpdateMethod method : updateMethods) {
            for(Device device : devices) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("device_" + device.getId() + "_update-method_" + method.getId());
            }
        }
        // Subscribe to the new topic to start receiving notifications.
        FirebaseMessaging.getInstance().subscribeToTopic("device_" + data.getDeviceId() + "_update-method_" + data.getUpdateMethodId());

    }
}
