package com.arjanvlek.oxygenupdater.utils

import android.app.Notification.CATEGORY_ERROR
import android.app.Notification.CATEGORY_PROGRESS
import android.app.Notification.CATEGORY_STATUS
import android.app.NotificationManager
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.arjanvlek.oxygenupdater.OxygenUpdater.Companion.PROGRESS_NOTIFICATION_CHANNEL_ID
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.activities.InstallActivity
import com.arjanvlek.oxygenupdater.activities.MainActivity
import com.arjanvlek.oxygenupdater.fragments.UpdateInformationFragment
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.NotificationIds.LOCAL_NOTIFICATION_CONTRIBUTION
import com.arjanvlek.oxygenupdater.utils.NotificationIds.LOCAL_NOTIFICATION_DOWNLOAD
import com.arjanvlek.oxygenupdater.utils.NotificationIds.LOCAL_NOTIFICATION_MD5_VERIFICATION
import org.koin.java.KoinJavaComponent.inject

object LocalNotifications {

    private const val TAG = "LocalNotifications"

    private val notificationManager by inject(NotificationManager::class.java)

    /**
     * Contribute: shows a notification that a update file has been submitted successfully.
     */
    fun showContributionSuccessfulNotification(
        context: Context,
        fileNameSet: Set<String>
    ) {
        try {
            val resultIntent = Intent(context, MainActivity::class.java)

            val stackBuilder = TaskStackBuilder.create(context)
                // Adds the back stack
                .addParentStack(MainActivity::class.java)
                // Adds the Intent to the top of the stack
                .addNextIntent(resultIntent)

            // Gets a PendingIntent containing the entire back stack
            val resultPendingIntent = stackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT)

            val title = context.getString(R.string.contribute_successful_notification_title)
            val text = context.getString(R.string.contribute_successful_notification_text)

            var inboxStyle = NotificationCompat.InboxStyle().addLine(text)
            fileNameSet.forEach {
                // Only the first 5-6 filenames will be shown
                inboxStyle = inboxStyle.addLine("\u2022 $it")
            }

            val notification = NotificationCompat.Builder(context, PROGRESS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(resultPendingIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setCategory(CATEGORY_STATUS)
                .setPriority(PRIORITY_LOW)
                .setStyle(inboxStyle)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .build()

            notificationManager.apply {
                notify(LOCAL_NOTIFICATION_CONTRIBUTION, notification)
            }
        } catch (e: Exception) {
            logError(TAG, "Can't display 'successful contribution' notification", e)
        }
    }

    /**
     * Shows a notification that the downloaded update file is downloaded successfully.
     */
    fun showDownloadCompleteNotification(
        context: Context,
        updateData: UpdateData?
    ) {
        try {
            // If the download complete notification is clicked, hide the first page of the install guide.
            val resultIntent = Intent(context, InstallActivity::class.java)
                .putExtra(InstallActivity.INTENT_SHOW_DOWNLOAD_PAGE, false)
                .putExtra(InstallActivity.INTENT_UPDATE_DATA, updateData)

            val stackBuilder = TaskStackBuilder.create(context)
                // Adds the back stack
                .addParentStack(MainActivity::class.java)
                // Adds the Intent to the top of the stack
                .addNextIntent(resultIntent)

            // Gets a PendingIntent containing the entire back stack
            val resultPendingIntent = stackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT)

            val title = context.getString(R.string.download_complete)
            val text = context.getString(R.string.download_complete_notification)

            val notification = NotificationCompat.Builder(context, PROGRESS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.download)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(resultPendingIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setCategory(CATEGORY_PROGRESS)
                .setPriority(PRIORITY_LOW)
                .setColor(ContextCompat.getColor(context, R.color.colorPositive))
                .setVisibility(VISIBILITY_PUBLIC)
                .build()

            notificationManager.apply {
                cancel(LOCAL_NOTIFICATION_MD5_VERIFICATION)
                notify(LOCAL_NOTIFICATION_DOWNLOAD, notification)
            }
        } catch (e: Exception) {
            logError(TAG, "Can't display 'download complete' notification", e)
        }
    }

    fun showDownloadFailedNotification(
        context: Context,
        resumable: Boolean,
        @StringRes message: Int,
        @StringRes notificationMessage: Int
    ) {
        try { // If the download complete notification is clicked, hide the first page of the install guide.
            val resultIntent = Intent(context, MainActivity::class.java)
                .putExtra(UpdateInformationFragment.KEY_HAS_DOWNLOAD_ERROR, true)
                .putExtra(UpdateInformationFragment.KEY_DOWNLOAD_ERROR_TITLE, context.getString(R.string.download_error))
                .putExtra(UpdateInformationFragment.KEY_DOWNLOAD_ERROR_MESSAGE, context.getString(message))
                .putExtra(UpdateInformationFragment.KEY_DOWNLOAD_ERROR_RESUMABLE, resumable)

            val stackBuilder = TaskStackBuilder.create(context)
                // Adds the back stack
                .addParentStack(MainActivity::class.java)
                // Adds the Intent to the top of the stack
                .addNextIntent(resultIntent)

            // Gets a PendingIntent containing the entire back stack
            val resultPendingIntent = stackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT)

            val title = context.getString(R.string.download_failed)
            val text = context.getString(notificationMessage)
            val bigTextStyle = NotificationCompat.BigTextStyle().bigText(text)

            val notification = NotificationCompat.Builder(context, PROGRESS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.download)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(resultPendingIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setCategory(CATEGORY_ERROR)
                .setPriority(PRIORITY_LOW)
                .setStyle(bigTextStyle)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setVisibility(VISIBILITY_PUBLIC)
                .build()

            notificationManager.apply {
                cancel(LOCAL_NOTIFICATION_MD5_VERIFICATION)
                notify(LOCAL_NOTIFICATION_DOWNLOAD, notification)
            }
        } catch (e: Exception) {
            logError(TAG, "Can't display download failed notification: ", e)
        }
    }

    fun showVerificationFailedNotification(context: Context) {
        val title = context.getString(R.string.download_verifying_error)
        val text = context.getString(R.string.download_notification_error_corrupt)
        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(text)

        val notification = NotificationCompat.Builder(context, PROGRESS_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(false)
            .setCategory(CATEGORY_ERROR)
            .setPriority(PRIORITY_LOW)
            .setStyle(bigTextStyle)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(VISIBILITY_PUBLIC)
            .build()

        notificationManager.apply {
            cancel(LOCAL_NOTIFICATION_DOWNLOAD)
            notify(LOCAL_NOTIFICATION_MD5_VERIFICATION, notification)
        }
    }

    /**
     * Shows a notification that the downloaded update file is being verified on MD5 sums.
     */
    fun showVerifyingNotification(context: Context) {
        try {
            val notification = NotificationCompat.Builder(context, PROGRESS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_notification)
                .setContentTitle(context.getString(R.string.download_verifying))
                .setProgress(100, 50, true)
                .setOngoing(true)
                .setCategory(CATEGORY_PROGRESS)
                .setPriority(PRIORITY_LOW)
                .setColor(ContextCompat.getColor(context, R.color.colorPositive))
                .setVisibility(VISIBILITY_PUBLIC)
                .build()

            notificationManager.apply {
                cancel(LOCAL_NOTIFICATION_DOWNLOAD)
                notify(LOCAL_NOTIFICATION_MD5_VERIFICATION, notification)
            }
        } catch (e: Exception) {
            logError(TAG, "Can't display 'verifying' notification", e)
        }
    }

    /**
     * Hides the download complete notification. Used when the install guide is manually clicked
     * from within the app.
     */
    fun hideDownloadCompleteNotification() {
        try {
            notificationManager.apply {
                cancel(LOCAL_NOTIFICATION_DOWNLOAD)
                cancel(LOCAL_NOTIFICATION_MD5_VERIFICATION)
            }
        } catch (e: Exception) {
            logError(TAG, "Can't hide 'download complete' notification", e)
        }
    }
}
