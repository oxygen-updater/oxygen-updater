package com.arjanvlek.oxygenupdater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.arjanvlek.oxygenupdater.Support.SettingsManager;

import java.util.Locale;

public class GcmNotificationListenerService extends com.google.android.gms.gcm.GcmListenerService {

    public static int NEW_UPDATE_NOTIFICATION_ID = 1;
    public static int NEW_DEVICE_NOTIFICATION_ID = 2;
    public static int MAINTENANCE_NOTIFICATION_ID = 3;

    /**
     * Show a notification when a message is received.
     * @param from From who the notification came (not used in this Application).
     * @param data Data bundle containing the message data.
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        showNotification(data);
    }

    /**
     * Displays a GCM notification
     * @param msg Bundle with GCM Notification Data.
     */
    private void showNotification(Bundle msg) {
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(NOTIFICATION_SERVICE);
        SettingsManager settingsManager = new SettingsManager(getApplicationContext());

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        String message = null;
        String messageType = "none";
        if (msg != null) {
            if (msg.getString("version_number") != null && msg.getString("device_name") != null && settingsManager.receiveSystemUpdateNotifications()) {
                String deviceName = msg.getString("device_name");
                message = getString(R.string.notification_version) + " " + msg.getString("version_number") + " " + getString(R.string.notification_is_now_available) + " " + deviceName + "!";
                messageType = "update";
            } else if (msg.getString("new_device") != null && settingsManager.receiveNewDeviceNotifications()) {
                String deviceName = msg.getString("new_device");
                message = getString(R.string.notification_new_device) + " " + deviceName + " " + getString(R.string.notification_new_device_2);
                messageType = "newDevice";
            } else if (msg.getString("version_number") == null && msg.getString("device_name") != null && settingsManager.receiveSystemUpdateNotifications()) {
                String deviceName = msg.getString("device_name");
                message = getString(R.string.notification_unknown_version_number) + " " + deviceName + "!";
                messageType = "update";

            } else if (msg.getString("server_message") != null && msg.getString("server_message_nl") != null && settingsManager.receiveWarningNotifications()) {
                String language = Locale.getDefault().getDisplayLanguage();
                switch (language) {
                    case "Nederlands":
                        message = msg.getString("server_message_nl");
                        break;
                    default:
                        message = msg.getString("server_message");
                        break;
                }
                messageType = "serverMessage";
            }
        }
        if (message != null) {
            switch (messageType) {
                case "update": {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(this)
                                    .setSmallIcon(R.drawable.ic_stat_notification_update)
                                    .setContentTitle(getString(R.string.app_name))
                                    .setStyle(new NotificationCompat.BigTextStyle()
                                            .bigText(message)
                                            .setSummaryText(getString(R.string.notification_update_short)))
                                    .setDefaults(Notification.DEFAULT_ALL)
                                    .setAutoCancel(true)
                                    .setContentText(message);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
                        mBuilder.setPriority(Notification.PRIORITY_HIGH);
                    }
                    mBuilder.setContentIntent(contentIntent);
                    mNotificationManager.notify(NEW_UPDATE_NOTIFICATION_ID, mBuilder.build());
                    break;
                }
                case "newDevice": {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(this)
                                    .setSmallIcon(R.drawable.ic_stat_notification_new_phone)
                                    .setContentTitle(getString(R.string.app_name))
                                    .setStyle(new NotificationCompat.BigTextStyle()
                                            .bigText(message)
                                            .setSummaryText(getString(R.string.notification_new_device_short)))
                                    .setDefaults(Notification.DEFAULT_ALL)
                                    .setAutoCancel(true)
                                    .setContentText(message);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
                        mBuilder.setPriority(Notification.PRIORITY_HIGH);
                    }
                    mBuilder.setContentIntent(contentIntent);
                    mNotificationManager.notify(NEW_DEVICE_NOTIFICATION_ID, mBuilder.build());
                    break;
                }
                case "serverMessage": {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(this)
                                    .setSmallIcon(R.drawable.ic_stat_notification_general)
                                    .setContentTitle(getString(R.string.app_name))
                                    .setStyle(new NotificationCompat.BigTextStyle()
                                            .bigText(message))
                                    .setDefaults(Notification.DEFAULT_ALL)
                                    .setAutoCancel(true)
                                    .setContentText(message);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
                        mBuilder.setPriority(Notification.PRIORITY_HIGH);
                    }
                    mBuilder.setContentIntent(contentIntent);
                    mNotificationManager.notify(MAINTENANCE_NOTIFICATION_ID, mBuilder.build());

                    break;
                }
            }
        }
    }
}