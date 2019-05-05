package com.arjanvlek.oxygenupdater.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.news.NewsActivity;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.views.MainActivity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static android.app.Notification.DEFAULT_ALL;
import static android.app.Notification.PRIORITY_HIGH;
import static com.arjanvlek.oxygenupdater.ApplicationData.LOCALE_DUTCH;
import static com.arjanvlek.oxygenupdater.news.NewsActivity.INTENT_NEWS_ITEM_ID;
import static com.arjanvlek.oxygenupdater.news.NewsActivity.INTENT_START_WITH_AD;
import static com.arjanvlek.oxygenupdater.notifications.NotificationElement.DEVICE_NAME;
import static com.arjanvlek.oxygenupdater.notifications.NotificationElement.DUTCH_MESSAGE;
import static com.arjanvlek.oxygenupdater.notifications.NotificationElement.ENGLISH_MESSAGE;
import static com.arjanvlek.oxygenupdater.notifications.NotificationElement.NEWS_ITEM_ID;
import static com.arjanvlek.oxygenupdater.notifications.NotificationElement.NEW_DEVICE_NAME;
import static com.arjanvlek.oxygenupdater.notifications.NotificationElement.NEW_VERSION_NUMBER;
import static com.arjanvlek.oxygenupdater.notifications.NotificationElement.TYPE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_NEWS_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS;

/**
 * Oxygen Updater - © 2018 Arjan Vlek
 */
public class DelayedPushNotificationDisplayer extends JobService {

    public static final int NEW_DEVICE_NOTIFICATION_ID = 10010;
    public static final int NEW_UPDATE_NOTIFICATION_ID = 20020;
    public static final int GENERIC_NOTIFICATION_ID = 30030;
    public static final int NEWS_NOTIFICATION_ID = 50050;
    public static final int UNKNOWN_NOTIFICATION_ID = 40040;

    public static final String KEY_NOTIFICATION_CONTENTS = "notification-contents";
    private static final String TAG = "DelayedPushNotificationDisplayer";

    @Override
    public boolean onStartJob(JobParameters params) {
        if (params == null || !params.getExtras().containsKey(KEY_NOTIFICATION_CONTENTS) || !(getApplication() instanceof ApplicationData)) {
            return exit(params, false);
        }

        SettingsManager settingsManager = new SettingsManager(getApplication());

        // Get notification contents of FCM.
        TypeReference<Map<String, String>> notificationContentsTypeRef = new TypeReference<Map<String, String>>() {
        };
        Map<String, String> messageContents;

        String notificationContentsJson = params.getExtras().getString(KEY_NOTIFICATION_CONTENTS);
        try {
            messageContents = new ObjectMapper().readValue(notificationContentsJson, notificationContentsTypeRef);
        } catch (IOException e) {
            Logger.logError(TAG, new OxygenUpdaterException("Failed to read notification contents from JSON string (" + notificationContentsJson + ")"));
            return exit(params, false);
        }
        NotificationType notificationType = NotificationType.valueOf(messageContents.get(TYPE.toString()));
        NotificationCompat.Builder builder = null;

        switch (notificationType) {
            case NEW_DEVICE:
                if (!settingsManager.getPreference(PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS, true)) {
                    return exit(params, true);
                }
                builder = getBuilderForNewDeviceNotification(messageContents.get(NEW_DEVICE_NAME.toString()));
                break;
            case NEW_VERSION:
                if (!settingsManager.getPreference(PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS, true)) {
                    return exit(params, true);
                }
                builder = getBuilderForNewVersionNotification(messageContents.get(DEVICE_NAME.toString()), messageContents.get(NEW_VERSION_NUMBER.toString()));
                break;
            case GENERAL_NOTIFICATION:
                if (!settingsManager.getPreference(PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS, true)) {
                    return exit(params, true);
                }

                String message;
                switch (Locale.getDefault().getDisplayLanguage()) {
                    case LOCALE_DUTCH:
                        message = messageContents.get(DUTCH_MESSAGE.toString());
                        break;
                    default:
                        message = messageContents.get(ENGLISH_MESSAGE.toString());
                        break;
                }
                builder = getBuilderForGeneralServerNotificationOrNewsNotification(message);
                break;
            case NEWS:
                if (!settingsManager.getPreference(PROPERTY_RECEIVE_NEWS_NOTIFICATIONS, true)) {
                    return exit(params, true);
                }

                String newsMessage;
                switch (Locale.getDefault().getDisplayLanguage()) {
                    case LOCALE_DUTCH:
                        newsMessage = messageContents.get(DUTCH_MESSAGE.toString());
                        break;
                    default:
                        newsMessage = messageContents.get(ENGLISH_MESSAGE.toString());
                        break;
                }
                builder = getBuilderForGeneralServerNotificationOrNewsNotification(newsMessage);
                break;

        }
        if (builder == null) {
            Logger.logError(TAG, new OxygenUpdaterException("Failed to instantiate notificationBuilder. Can not display push notification!"));
            return exit(params, false);
        }

        builder.setContentIntent(getNotificationIntent(notificationType, messageContents));
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setPriority(PRIORITY_HIGH);
        builder.setDefaults(DEFAULT_ALL);
        builder.setAutoCancel(true);

        int notificationId = getNotificationId(notificationType);
        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) Utils.getSystemService(this, NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Logger.logError(TAG, new OxygenUpdaterException("Notification Manager service is not available"));
            return exit(params, false);
        }

        notificationManager.notify(notificationId, notification);
        return exit(params, true);
    }

    private int getNotificationId(NotificationType type) {
        switch (type) {
            case NEW_DEVICE:
                return NEW_DEVICE_NOTIFICATION_ID;
            case NEW_VERSION:
                return NEW_UPDATE_NOTIFICATION_ID;
            case GENERAL_NOTIFICATION:
                return GENERIC_NOTIFICATION_ID;
            case NEWS:
                return NEWS_NOTIFICATION_ID;
            default:
                return UNKNOWN_NOTIFICATION_ID;
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private NotificationCompat.Builder getBuilderForGeneralServerNotificationOrNewsNotification(String message) {
        return getNotificationBuilder()
                .setSmallIcon(R.drawable.ic_stat_notification_general)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message);
    }

    private NotificationCompat.Builder getBuilderForNewDeviceNotification(String newDeviceName) {
        String message = getString(R.string.notification_new_device, newDeviceName);

        return getNotificationBuilder()
                .setSmallIcon(R.drawable.ic_stat_notification_new_device)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message)
                        .setSummaryText(getString(R.string.notification_new_device_short)))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message);
    }

    private NotificationCompat.Builder getBuilderForNewVersionNotification(String deviceName, String versionNumber) {
        String message = getString(R.string.notification_version, versionNumber, deviceName);
        return getNotificationBuilder()
                .setSmallIcon(R.drawable.ic_stat_notification_new_version)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getString(R.string.notification_version_title))
                .setContentText(message);
    }


    private NotificationCompat.Builder getNotificationBuilder() {
        if (Build.VERSION.SDK_INT >= 26) {
            return new NotificationCompat.Builder(this, ApplicationData.PUSH_NOTIFICATION_CHANNEL_ID);
        } else {
            //noinspection deprecation - Only runs on older Android versions.
            return new NotificationCompat.Builder(this);
        }
    }

    private PendingIntent getNotificationIntent(NotificationType notificationType, Map<String, String> messageContents) {
        PendingIntent contentIntent;

        if (notificationType == NotificationType.NEWS) {
            Intent newsIntent = new Intent(this, NewsActivity.class);
            newsIntent.putExtra(INTENT_NEWS_ITEM_ID, Long.valueOf(messageContents.get(NEWS_ITEM_ID.toString())));
            newsIntent.putExtra(INTENT_START_WITH_AD, true);
            contentIntent = PendingIntent.getActivity(this, 0, newsIntent, 0);
        } else {
            contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        }

        return contentIntent;
    }

    private boolean exit(JobParameters parameters, boolean success) {
        jobFinished(parameters, !success);
        return success;
    }
}
