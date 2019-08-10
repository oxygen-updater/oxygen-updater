package com.arjanvlek.oxygenupdater.notifications


import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.download.DownloadProgressData
import com.arjanvlek.oxygenupdater.installation.InstallActivity
import com.arjanvlek.oxygenupdater.installation.InstallActivity.Companion.INTENT_SHOW_DOWNLOAD_PAGE
import com.arjanvlek.oxygenupdater.installation.InstallActivity.Companion.INTENT_UPDATE_DATA
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData
import com.arjanvlek.oxygenupdater.updateinformation.UpdateInformationFragment.Companion.KEY_DOWNLOAD_ERROR_MESSAGE
import com.arjanvlek.oxygenupdater.updateinformation.UpdateInformationFragment.Companion.KEY_DOWNLOAD_ERROR_RESUMABLE
import com.arjanvlek.oxygenupdater.updateinformation.UpdateInformationFragment.Companion.KEY_DOWNLOAD_ERROR_TITLE
import com.arjanvlek.oxygenupdater.updateinformation.UpdateInformationFragment.Companion.KEY_HAS_DOWNLOAD_ERROR
import com.arjanvlek.oxygenupdater.versionformatter.UpdateDataVersionFormatter
import com.arjanvlek.oxygenupdater.views.MainActivity


object LocalNotifications {

    const val NOT_ONGOING = false
    const val ONGOING = true
    const val HAS_NO_ERROR = false
    const val HAS_ERROR = true
    private const val VERIFYING_NOTIFICATION_ID = 500000000
    private const val DOWNLOAD_COMPLETE_NOTIFICATION_ID = 1000000000
    private const val DOWNLOADING_NOTIFICATION_ID = 1500000000
    private const val DOWNLOAD_FAILED_NOTIFICATION_ID = 200000000
    private const val CONTRIBUTE_SUCCESSFUL_NOTIFICATION_ID = 250000000
    private const val TAG = "LocalNotifications"

    /**
     * Shows a notification that an update is downloading
     */
    fun showDownloadingNotification(context: Context, updateData: UpdateData, downloadProgressData: DownloadProgressData) {
        try {
            val builder = NotificationCompat.Builder(context, ApplicationData.PROGRESS_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true)
                    .setContentTitle(UpdateDataVersionFormatter.getFormattedVersionNumber(updateData))
                    .setStyle(NotificationCompat.BigTextStyle()
                            .setBigContentTitle(UpdateDataVersionFormatter.getFormattedVersionNumber(updateData))
                            .bigText(if (downloadProgressData.timeRemaining != null) downloadProgressData.timeRemaining.toString(context) else "")
                    )
                    .setProgress(100, downloadProgressData.progress, false)

            builder.setCategory(Notification.CATEGORY_PROGRESS)

            val manager = Utils.getSystemService(context, Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(DOWNLOAD_COMPLETE_NOTIFICATION_ID)
            manager.cancel(DOWNLOAD_FAILED_NOTIFICATION_ID)
            manager.cancel(VERIFYING_NOTIFICATION_ID)
            manager.notify(DOWNLOADING_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            logError(TAG, "Can't display 'downloading' notification", e)
        }

    }

    /**
     * Shows a notification that the download has been paused.
     */
    fun showDownloadPausedNotification(context: Context, updateData: UpdateData, downloadProgressData: DownloadProgressData) {
        try {
            // If the download-in-progress notification is clicked, go to the app itself
            val resultIntent = Intent(context, MainActivity::class.java)

            val stackBuilder = TaskStackBuilder.create(context)
            // Adds the back stack
            stackBuilder.addParentStack(MainActivity::class.java)
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent)
            // Gets a PendingIntent containing the entire back stack
            val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(context, ApplicationData.PROGRESS_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(true)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(false)
                    .setContentTitle(UpdateDataVersionFormatter.getFormattedVersionNumber(updateData))
                    .setProgress(100, downloadProgressData.progress, false)
                    .setStyle(NotificationCompat.BigTextStyle()
                            .setBigContentTitle(UpdateDataVersionFormatter.getFormattedVersionNumber(updateData))
                            .bigText(downloadProgressData.progress.toString() + "%, "
                                    + if (downloadProgressData.isWaitingForConnection)
                                context.getString(R.string.download_waiting_for_network)
                            else
                                context.getString(R.string.paused)
                            )
                    )

            builder.setCategory(Notification.CATEGORY_PROGRESS)

            val manager = Utils.getSystemService(context, Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(DOWNLOADING_NOTIFICATION_ID, builder.build()) // Same as downloading so we can't have both a downloading and paused notification.
        } catch (e: Exception) {
            logError(TAG, "Can't display 'download paused' notification", e)
        }

    }

    /**
     * Shows a notification that the downloaded update file is downloaded successfully.
     */
    fun showDownloadCompleteNotification(context: Context, updateData: UpdateData) {
        try {
            // If the download complete notification is clicked, hide the first page of the install guide.
            val resultIntent = Intent(context, InstallActivity::class.java)
            resultIntent.putExtra(INTENT_SHOW_DOWNLOAD_PAGE, false)
            resultIntent.putExtra(INTENT_UPDATE_DATA, updateData)

            val stackBuilder = TaskStackBuilder.create(context)
            // Adds the back stack
            stackBuilder.addParentStack(MainActivity::class.java)
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent)
            // Gets a PendingIntent containing the entire back stack
            val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(context, ApplicationData.PROGRESS_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.download_complete_notification))

            builder.setCategory(Notification.CATEGORY_SYSTEM)

            val manager = Utils.getSystemService(context, Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(DOWNLOADING_NOTIFICATION_ID)
            manager.cancel(DOWNLOAD_FAILED_NOTIFICATION_ID)
            manager.cancel(VERIFYING_NOTIFICATION_ID)
            manager.notify(DOWNLOAD_COMPLETE_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            logError(TAG, "Can't display 'download complete' notification", e)
        }

    }

    /**
     * Contribute: shows a notification that a update file has been submitted successfully.
     */
    fun showContributionSuccessfulNotification(context: Context, filename: String) {
        try {
            // If this notification is clicked, open the app.
            val resultIntent = Intent(context, MainActivity::class.java)

            val stackBuilder = TaskStackBuilder.create(context)
            // Adds the back stack
            stackBuilder.addParentStack(MainActivity::class.java)
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent)
            // Gets a PendingIntent containing the entire back stack
            val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(context, ApplicationData.PROGRESS_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                    .setOngoing(false)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle(context.getString(R.string.contribute_successful_notification_title))
                    .setContentText(context.getString(R.string.contribute_successful_notification_text, filename))

            builder.setCategory(Notification.CATEGORY_SYSTEM)

            val manager = Utils.getSystemService(context, Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(CONTRIBUTE_SUCCESSFUL_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            logError(TAG, "Can't display 'successful contribution' notification", e)
        }

    }

    /**
     * Hides the downloading notification. Used when the download is cancelled by the user.
     */
    fun hideDownloadingNotification(context: Context) {
        try {
            val manager = Utils.getSystemService(context, Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(DOWNLOADING_NOTIFICATION_ID)
            manager.cancel(DOWNLOAD_COMPLETE_NOTIFICATION_ID)
            manager.cancel(VERIFYING_NOTIFICATION_ID)
        } catch (e: Exception) {
            logError(TAG, "Can't hide 'downloading' notification", e)
        }

    }

    /**
     * Hides the download complete notification. Used when the install guide is manually clicked
     * from within the app.
     */
    fun hideDownloadCompleteNotification(context: Context) {
        try {
            val manager = Utils.getSystemService(context, Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(DOWNLOAD_COMPLETE_NOTIFICATION_ID)
        } catch (e: Exception) {
            logError(TAG, "Can't hide 'download complete' notification", e)
        }

    }


    fun showDownloadFailedNotification(context: Context, resumable: Boolean, @StringRes message: Int, @StringRes notificationMessage: Int) {
        try {
            // If the download complete notification is clicked, hide the first page of the install guide.
            val resultIntent = Intent(context, MainActivity::class.java)
            resultIntent.putExtra(KEY_HAS_DOWNLOAD_ERROR, true)
            resultIntent.putExtra(KEY_DOWNLOAD_ERROR_TITLE, context.getString(R.string.download_error))
            resultIntent.putExtra(KEY_DOWNLOAD_ERROR_MESSAGE, context.getString(message))
            resultIntent.putExtra(KEY_DOWNLOAD_ERROR_RESUMABLE, resumable)

            val stackBuilder = TaskStackBuilder.create(context)
            // Adds the back stack
            stackBuilder.addParentStack(MainActivity::class.java)
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent)
            // Gets a PendingIntent containing the entire back stack
            val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(context, ApplicationData.PROGRESS_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle(context.getString(R.string.download_failed))
                    .setContentText(context.getString(notificationMessage))

            builder.setCategory(Notification.CATEGORY_SYSTEM)

            val manager = Utils.getSystemService(context, Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(DOWNLOADING_NOTIFICATION_ID)
            manager.cancel(DOWNLOAD_COMPLETE_NOTIFICATION_ID)
            manager.cancel(VERIFYING_NOTIFICATION_ID)
            manager.notify(DOWNLOAD_FAILED_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            logError(TAG, "Can't display download failed notification: ", e)
        }

    }

    /**
     * Shows a notification that the downloaded update file is being verified on MD5 sums.
     *
     * @param error If an error occurred during verification, display an error text in the
     * notification.
     */
    fun showVerifyingNotification(context: Context, ongoing: Boolean, error: Boolean) {
        try {
            val builder = NotificationCompat.Builder(context, ApplicationData.PROGRESS_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(if (ongoing) android.R.drawable.stat_sys_download else android.R.drawable.stat_sys_download_done)
                    .setOngoing(ongoing)

            if (ongoing) {
                builder.setProgress(100, 50, true)
            }

            if (error) {
                builder.setContentTitle(context.getString(R.string.download_verifying_error))
                builder.setContentTitle(context.getString(R.string.download_notification_error_corrupt))
            } else {
                builder.setContentTitle(context.getString(R.string.download_verifying))
            }

            builder.setCategory(Notification.CATEGORY_PROGRESS)
            val manager = Utils.getSystemService(context, Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(DOWNLOADING_NOTIFICATION_ID)
            manager.cancel(DOWNLOAD_COMPLETE_NOTIFICATION_ID)
            manager.cancel(DOWNLOAD_FAILED_NOTIFICATION_ID)
            manager.notify(VERIFYING_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            logError(TAG, "Can't display 'verifying' (still going: $ongoing, verification failed: $error) notification", e)
        }

    }

    /**
     * Hides the verifying notification. Used when verification has succeeded.
     */
    fun hideVerifyingNotification(context: Context) {
        try {
            val manager = Utils.getSystemService(context, Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(VERIFYING_NOTIFICATION_ID)
        } catch (e: Exception) {
            logError(TAG, "Can't hide 'verifying' notification", e)
        }

    }
}
