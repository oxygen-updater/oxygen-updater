package com.oxygenupdater.utils

import android.app.Notification.CATEGORY_ERROR
import android.app.Notification.CATEGORY_PROGRESS
import android.app.Notification.CATEGORY_STATUS
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.oxygenupdater.R
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.extensions.setBigTextStyle
import com.oxygenupdater.extensions.tryNotify
import com.oxygenupdater.ui.main.ChildScreen
import com.oxygenupdater.ui.main.DownloadedArg
import com.oxygenupdater.ui.main.OuSchemeSuffixed
import com.oxygenupdater.ui.update.KeyDownloadErrorMessage
import com.oxygenupdater.ui.update.KeyDownloadErrorResumable
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.DownloadStatusNotifChannelId
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.VerificationStatusNotifChannelId
import com.oxygenupdater.utils.NotificationChannels.MiscellaneousGroup.OtaUrlSubmittedNotifChannelId

object LocalNotifications {

    /**
     * Contribute: shows a notification that a update file has been submitted successfully.
     */
    fun showContributionSuccessfulNotification(context: Context, filenameSet: Set<String>) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            // Since MainActivity's `launchMode` is `singleTask`, we don't
            // need to add any flags to avoid creating multiple instances
            Intent(context, MainActivity::class.java),
            FLAG_UPDATE_CURRENT or if (SDK_INT >= VERSION_CODES.M) FLAG_IMMUTABLE else 0
        )

        val title = context.getString(R.string.contribute_successful_notification_title)
        val text = context.getString(R.string.contribute_successful_notification_text)

        val inboxStyle = NotificationCompat.InboxStyle().addLine(text)
        filenameSet.forEach {
            // Only the first 5-6 filenames will be shown
            inboxStyle.addLine("• $it")
        }

        val notification = NotificationCompat.Builder(context, OtaUrlSubmittedNotifChannelId)
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

        with(NotificationManagerCompat.from(context)) {
            tryNotify(NotificationIds.LocalContribution, notification)
        }
    }

    /**
     * Shows a notification that the downloaded update file is downloaded successfully.
     */
    fun showDownloadCompleteNotification(context: Context) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(OuSchemeSuffixed + ChildScreen.Guide.value + "$DownloadedArg=true"),
                context,
                MainActivity::class.java
            ),
            FLAG_UPDATE_CURRENT or if (SDK_INT >= VERSION_CODES.M) FLAG_IMMUTABLE else 0
        )

        val title = context.getString(R.string.download_complete)
        val text = context.getString(R.string.download_complete_notification)

        val notification = NotificationCompat.Builder(context, DownloadStatusNotifChannelId)
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

        with(NotificationManagerCompat.from(context)) {
            cancel(NotificationIds.LocalMd5Verification)
            tryNotify(NotificationIds.LocalDownload, notification)
        }
    }

    fun showDownloadFailedNotification(
        context: Context,
        resumable: Boolean,
        @StringRes message: Int,
        @StringRes notificationMessage: Int,
    ) {
        // Since MainActivity's `launchMode` is `singleTask`, we don't
        // need to add any flags to avoid creating multiple instances
        val intent = Intent(context, MainActivity::class.java)
            // Show a dialog detailing the download failure
            .putExtra(KeyDownloadErrorMessage, context.getString(message))
            .putExtra(KeyDownloadErrorResumable, resumable)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            FLAG_UPDATE_CURRENT or if (SDK_INT >= VERSION_CODES.M) FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(context, DownloadStatusNotifChannelId)
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

        with(NotificationManagerCompat.from(context)) {
            cancel(NotificationIds.LocalMd5Verification)
            tryNotify(NotificationIds.LocalDownload, notification)
        }
    }

    fun showVerificationFailedNotification(context: Context) {
        val title = context.getString(R.string.download_verifying_error)
        val text = context.getString(R.string.download_notification_error_corrupt)

        val notification = NotificationCompat.Builder(context, VerificationStatusNotifChannelId)
            .setSmallIcon(R.drawable.logo_notification)
            .setContentTitle(title)
            .setBigTextStyle(text)
            .setOngoing(false)
            .setCategory(CATEGORY_ERROR)
            .setPriority(PRIORITY_LOW)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(VISIBILITY_PUBLIC)
            .build()

        with(NotificationManagerCompat.from(context)) {
            cancel(NotificationIds.LocalDownload)
            tryNotify(NotificationIds.LocalMd5Verification, notification)
        }
    }

    /**
     * Shows a notification that the downloaded update file is being verified on MD5 sums.
     */
    fun showVerifyingNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, VerificationStatusNotifChannelId)
            .setSmallIcon(R.drawable.logo_notification)
            .setContentTitle(context.getString(R.string.download_verifying))
            .setProgress(100, 50, true)
            .setOngoing(true)
            .setCategory(CATEGORY_PROGRESS)
            .setPriority(PRIORITY_LOW)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(VISIBILITY_PUBLIC)
            .build()

        with(NotificationManagerCompat.from(context)) {
            cancel(NotificationIds.LocalDownload)
            tryNotify(NotificationIds.LocalMd5Verification, notification)
        }
    }

    fun hideDownloadCompleteNotification(context: Context) = with(NotificationManagerCompat.from(context)) {
        cancel(NotificationIds.LocalDownload)
        cancel(NotificationIds.LocalMd5Verification)
    }
}
