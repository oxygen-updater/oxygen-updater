package com.arjanvlek.oxygenupdater.notifications;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.installation.InstallActivity;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData;
import com.arjanvlek.oxygenupdater.views.MainActivity;

import static com.arjanvlek.oxygenupdater.installation.InstallActivity.INTENT_SHOW_DOWNLOAD_PAGE;
import static com.arjanvlek.oxygenupdater.installation.InstallActivity.INTENT_UPDATE_DATA;


public class LocalNotifications {

    private static final int VERIFYING_NOTIFICATION_ID = 500000000;
    private static final int DOWNLOAD_COMPLETE_NOTIFICATION_ID = 1000000000;
    private static final int DOWNLOAD_FAILED_NOTIFICATION_ID = 200000000;

    private static final String KEY_HAS_DOWNLOAD_ERROR = "has_download_error";
    private static final String KEY_DOWNLOAD_ERROR_TITLE = "download_error_title";
    private static final String KEY_DOWNLOAD_ERROR_MESSAGE = "download_error_message";
    private static final String TAG = "LocalNotifications";

    public static final boolean NOT_ONGOING = false;
    public static final boolean ONGOING = true;

    public static final boolean HAS_NO_ERROR = false;
    public static final boolean HAS_ERROR = true;


    /**
     * Shows a notification that the downloaded update file is downloaded successfully.
     */
    public static void showDownloadCompleteNotification(Context context, UpdateData updateData) {
        try {
            // If the download complete notification is clicked, hide the first page of the install guide.
            Intent resultIntent = new Intent(context, InstallActivity.class);
            resultIntent.putExtra(INTENT_SHOW_DOWNLOAD_PAGE, false);
            resultIntent.putExtra(INTENT_UPDATE_DATA, updateData);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            // Adds the back stack
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            // Gets a PendingIntent containing the entire back stack
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ApplicationData.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.download_complete_notification));

            if (Build.VERSION.SDK_INT >= 21) {
                builder.setCategory(Notification.CATEGORY_SYSTEM);
            }

            NotificationManager manager = (NotificationManager) Utils.getSystemService(context, Context.NOTIFICATION_SERVICE);
            manager.notify(DOWNLOAD_COMPLETE_NOTIFICATION_ID, builder.build());
        } catch(Exception e) {
            Logger.logError(TAG, "Can't display download complete notification: ", e);
        }
    }


    /**
     * Hides the download complete notification. Used when the install guide is manually clicked from within the app.
     */
    public static void hideDownloadCompleteNotification(Context context) {
        try {
            NotificationManager manager = (NotificationManager) Utils.getSystemService(context, Context.NOTIFICATION_SERVICE);
            manager.cancel(DOWNLOAD_COMPLETE_NOTIFICATION_ID);
        } catch(Exception e) {
            Logger.logError(TAG, "Can't hide download complete notification: ", e);
        }
    }


    public static void showDownloadFailedNotification(Context context, @StringRes int message, @StringRes int notificationMessage) {
        try {
            // If the download complete notification is clicked, hide the first page of the install guide.
            Intent resultIntent = new Intent(context, MainActivity.class);
            resultIntent.putExtra(KEY_HAS_DOWNLOAD_ERROR, true);
            resultIntent.putExtra(KEY_DOWNLOAD_ERROR_TITLE, context.getString(R.string.download_failed));
            resultIntent.putExtra(KEY_DOWNLOAD_ERROR_MESSAGE, context.getString(message));

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            // Adds the back stack
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            // Gets a PendingIntent containing the entire back stack
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ApplicationData.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle(context.getString(R.string.download_failed))
                    .setContentText(context.getString(notificationMessage));

            if (Build.VERSION.SDK_INT >= 21) {
                builder.setCategory(Notification.CATEGORY_SYSTEM);
            }

            NotificationManager manager = (NotificationManager) Utils.getSystemService(context, Context.NOTIFICATION_SERVICE);
            manager.notify(DOWNLOAD_FAILED_NOTIFICATION_ID, builder.build());
        } catch(Exception e) {
            Logger.logError(TAG, "Can't display download failed notification: ", e);
        }
    }

    /**
     * Shows a notification that the downloaded update file is being verified on MD5 sums.
     * @param error If an error occurred during verification, display an error text in the notification.
     */
    public static void showVerifyingNotification(Context context, boolean ongoing, boolean error) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ApplicationData.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(ongoing ? android.R.drawable.stat_sys_download : android.R.drawable.stat_sys_download_done)
                    .setOngoing(ongoing);

            if (ongoing) {
                builder.setProgress(100, 50, true);
            }

            if(error) {
                builder.setContentTitle(context.getString(R.string.download_verifying_error));
                builder.setContentTitle(context.getString(R.string.download_notification_error_corrupt));
            } else {
                builder.setContentTitle(context.getString(R.string.download_verifying));
            }

            if (Build.VERSION.SDK_INT >= 21) {
                builder.setCategory(Notification.CATEGORY_PROGRESS);
            }
            NotificationManager manager = (NotificationManager) Utils.getSystemService(context, Context.NOTIFICATION_SERVICE);
            manager.notify(VERIFYING_NOTIFICATION_ID, builder.build());
        } catch(Exception e) {
            Logger.logError(TAG, "Can't display verifying (ongoing: " + ongoing + ", error: " + error + ") notification: ", e);
        }
    }

    /**
     * Hides the verifying notification. Used when verification has succeeded.
     */
    public static void hideVerifyingNotification(Context context) {
        try {
            NotificationManager manager = (NotificationManager) Utils.getSystemService(context, Context.NOTIFICATION_SERVICE);
            manager.cancel(VERIFYING_NOTIFICATION_ID);
        } catch(Exception e) {
            Logger.logError(TAG, "Can't hide verifying notification: ", e);
        }
    }
}
