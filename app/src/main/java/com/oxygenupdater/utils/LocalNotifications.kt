package com.oxygenupdater.utils

import android.app.Notification.CATEGORY_ERROR
import android.app.Notification.CATEGORY_PROGRESS
import android.app.Notification.CATEGORY_STATUS
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.oxygenupdater.R
import com.oxygenupdater.activities.InstallActivity
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.extensions.setBigTextStyle
import com.oxygenupdater.fragments.UpdateInformationFragment
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.VERIFICATION_STATUS_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.MiscellaneousGroup.OTA_URL_SUBMITTED_NOTIFICATION_CHANNEL_ID
import org.koin.java.KoinJavaComponent.getKoin

object LocalNotifications {

    private const val TAG = "LocalNotifications"

    private val notificationManager by getKoin().inject<NotificationManagerCompat>()

    /**
     * Contribute: shows a notification that a update file has been submitted successfully.
     */
    fun showContributionSuccessfulNotification(
        context: Context,
        filenameSet: Set<String>
    ) = try {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            // Since MainActivity's `launchMode` is `singleTask`, we don't
            // need to add any flags to avoid creating multiple instances
            Intent(context, MainActivity::class.java),
            FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                FLAG_MUTABLE
            } else 0
        )

        val title = context.getString(R.string.contribute_successful_notification_title)
        val text = context.getString(R.string.contribute_successful_notification_text)

        val inboxStyle = NotificationCompat.InboxStyle().addLine(text)
        filenameSet.forEach {
            // Only the first 5-6 filenames will be shown
            inboxStyle.addLine("\u2022 $it")
        }

        val notification = NotificationCompat.Builder(context, OTA_URL_SUBMITTED_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setCategory(CATEGORY_STATUS)
            .setPriority(PRIORITY_LOW)
            .setStyle(inboxStyle)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .build()

        notificationManager.notify(NotificationIds.LOCAL_CONTRIBUTION, notification)
    } catch (e: Exception) {
        logError(TAG, "Can't display 'successful contribution' notification", e)
    }

    /**
     * Shows a notification that the downloaded update file is downloaded successfully.
     */
    fun showDownloadCompleteNotification(context: Context) {
        try {
            val intent = Intent(context, InstallActivity::class.java)
                // Hide the first page of the install guide because that page
                // contains download instructions, which will be redundant because
                // the download has already completed
                .putExtra(InstallActivity.INTENT_SHOW_DOWNLOAD_PAGE, false)

            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    FLAG_MUTABLE
                } else 0
            )

            val title = context.getString(R.string.download_complete)
            val text = context.getString(R.string.download_complete_notification)

            val notification = NotificationCompat.Builder(context, DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.download)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setCategory(CATEGORY_PROGRESS)
                .setPriority(PRIORITY_LOW)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setVisibility(VISIBILITY_PUBLIC)
                .build()

            notificationManager.apply {
                cancel(NotificationIds.LOCAL_MD5_VERIFICATION)
                notify(NotificationIds.LOCAL_DOWNLOAD, notification)
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
        try {
            // Since MainActivity's `launchMode` is `singleTask`, we don't
            // need to add any flags to avoid creating multiple instances
            val intent = Intent(context, MainActivity::class.java)
                // Show a dialog detailing the download failure
                .putExtra(UpdateInformationFragment.KEY_HAS_DOWNLOAD_ERROR, true)
                .putExtra(UpdateInformationFragment.KEY_DOWNLOAD_ERROR_TITLE, context.getString(R.string.download_error))
                .putExtra(UpdateInformationFragment.KEY_DOWNLOAD_ERROR_MESSAGE, context.getString(message))
                .putExtra(UpdateInformationFragment.KEY_DOWNLOAD_ERROR_RESUMABLE, resumable)

            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    FLAG_MUTABLE
                } else 0
            )

            val notification = NotificationCompat.Builder(context, DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.download)
                .setContentTitle(context.getString(R.string.download_failed))
                .setBigTextStyle(context.getString(notificationMessage))
                .setContentIntent(contentIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setCategory(CATEGORY_ERROR)
                .setPriority(PRIORITY_LOW)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setVisibility(VISIBILITY_PUBLIC)
                .build()

            notificationManager.apply {
                cancel(NotificationIds.LOCAL_MD5_VERIFICATION)
                notify(NotificationIds.LOCAL_DOWNLOAD, notification)
            }
        } catch (e: Exception) {
            logError(TAG, "Can't display download failed notification: ", e)
        }
    }

    fun showVerificationFailedNotification(context: Context) {
        val title = context.getString(R.string.download_verifying_error)
        val text = context.getString(R.string.download_notification_error_corrupt)

        val notification = NotificationCompat.Builder(context, VERIFICATION_STATUS_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_notification)
            .setContentTitle(title)
            .setBigTextStyle(text)
            .setOngoing(false)
            .setCategory(CATEGORY_ERROR)
            .setPriority(PRIORITY_LOW)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(VISIBILITY_PUBLIC)
            .build()

        notificationManager.apply {
            cancel(NotificationIds.LOCAL_DOWNLOAD)
            notify(NotificationIds.LOCAL_MD5_VERIFICATION, notification)
        }
    }

    /**
     * Shows a notification that the downloaded update file is being verified on MD5 sums.
     */
    fun showVerifyingNotification(context: Context) {
        try {
            val notification = NotificationCompat.Builder(context, VERIFICATION_STATUS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_notification)
                .setContentTitle(context.getString(R.string.download_verifying))
                .setProgress(100, 50, true)
                .setOngoing(true)
                .setCategory(CATEGORY_PROGRESS)
                .setPriority(PRIORITY_LOW)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setVisibility(VISIBILITY_PUBLIC)
                .build()

            notificationManager.apply {
                cancel(NotificationIds.LOCAL_DOWNLOAD)
                notify(NotificationIds.LOCAL_MD5_VERIFICATION, notification)
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
                cancel(NotificationIds.LOCAL_DOWNLOAD)
                cancel(NotificationIds.LOCAL_MD5_VERIFICATION)
            }
        } catch (e: Exception) {
            logError(TAG, "Can't hide 'download complete' notification", e)
        }
    }
}
