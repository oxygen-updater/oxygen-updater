package com.arjanvlek.oxygenupdater.notifications;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.views.InstallGuideActivity;
import com.arjanvlek.oxygenupdater.views.MainActivity;

import static com.arjanvlek.oxygenupdater.views.InstallGuideActivity.INTENT_SHOW_DOWNLOAD_PAGE;


public class LocalNotifications {

    private static final int VERIFYING_NOTIFICATION_ID = 500000000;
    private static final int DOWNLOAD_COMPLETE_NOTIFICATION_ID = 1000000000;
    private static final int DOWNLOAD_FAILED_NOTIFICATION_ID = 200000000;

    private static final String KEY_HAS_DOWNLOAD_ERROR = "has_download_error";
    private static final String KEY_DOWNLOAD_ERROR_TITLE = "download_error_title";
    private static final String KEY_DOWNLOAD_ERROR_MESSAGE = "download_error_message";

    public static final boolean NOT_ONGOING = false;
    public static final boolean ONGOING = true;

    public static final boolean HAS_NO_ERROR = false;
    public static final boolean HAS_ERROR = true;


    /**
     * Shows a notification that the downloaded update file is downloaded successfully.
     */
    public static void showDownloadCompleteNotification(Activity activity) {
        try {
            // If the download complete notification is clicked, hide the first page of the install guide.
            Intent resultIntent = new Intent(activity, InstallGuideActivity.class);
            resultIntent.putExtra(INTENT_SHOW_DOWNLOAD_PAGE, false);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
            // Adds the back stack
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            // Gets a PendingIntent containing the entire back stack
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(activity)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle(activity.getString(R.string.app_name))
                    .setContentText(activity.getString(R.string.download_complete_notification));

            if (Build.VERSION.SDK_INT >= 21) {
                builder.setCategory(Notification.CATEGORY_SYSTEM);
            }

            NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(DOWNLOAD_COMPLETE_NOTIFICATION_ID, builder.build());
        } catch(Exception ignored) {

        }
    }


    /**
     * Hides the download complete notification. Used when the install guide is manually clicked from within the app.
     */
    public static void hideDownloadCompleteNotification(Activity activity) {
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(DOWNLOAD_COMPLETE_NOTIFICATION_ID);
    }


    public static void showDownloadFailedNotification(Activity activity, @StringRes int message, @StringRes int notificationMessage) {
        // If the download complete notification is clicked, hide the first page of the install guide.
        Intent resultIntent = new Intent(activity, MainActivity.class);
        resultIntent.putExtra(KEY_HAS_DOWNLOAD_ERROR, true);
        resultIntent.putExtra(KEY_DOWNLOAD_ERROR_TITLE, activity.getString(R.string.download_failed));
        resultIntent.putExtra(KEY_DOWNLOAD_ERROR_MESSAGE, activity.getString(message));

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
        // Adds the back stack
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(false)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .setContentTitle(activity.getString(R.string.download_failed))
                .setContentText(activity.getString(notificationMessage));

        if (Build.VERSION.SDK_INT >= 21) {
            builder.setCategory(Notification.CATEGORY_SYSTEM);
        }

        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(DOWNLOAD_FAILED_NOTIFICATION_ID, builder.build());
    }



    /**
     * Shows a notification that the downloaded update file is being verified on MD5 sums.
     * @param error If an error occurred during verification, display an error text in the notification.
     */
    public static void showVerifyingNotification(Activity activity, boolean ongoing, boolean error) {
        NotificationCompat.Builder builder;
        try {
            builder = new NotificationCompat.Builder(activity)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(ongoing);

            if (ongoing) {
                builder.setProgress(100, 50, true);
            }

            if(error) {
                builder.setContentTitle(activity.getString(R.string.download_verifying_error));
                builder.setContentTitle(activity.getString(R.string.download_notification_error_corrupt));
            } else {
                builder.setContentTitle(activity.getString(R.string.download_verifying));
            }

            if (Build.VERSION.SDK_INT >= 21) {
                builder.setCategory(Notification.CATEGORY_PROGRESS);
            }
            NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(VERIFYING_NOTIFICATION_ID, builder.build());
        } catch(Exception e) {
            try {
                NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancel(VERIFYING_NOTIFICATION_ID);
            } catch(Exception ignored) {

            }
        }
    }


    /**
     * Hides the verifying notification. Used when verification has succeeded.
     */
    public static void hideVerifyingNotification(Activity activity) {
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(VERIFYING_NOTIFICATION_ID);
    }
}
