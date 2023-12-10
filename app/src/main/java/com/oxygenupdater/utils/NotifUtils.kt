package com.oxygenupdater.utils

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.Size
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationManagerCompat
import com.oxygenupdater.R
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup
import com.oxygenupdater.utils.NotificationChannels.MiscellaneousGroup
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup

object NotifUtils {

    private fun NotificationManagerCompat.isDisabled(
        channelId: String,
    ) = !areNotificationsEnabled() || getNotificationChannel(channelId)?.run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            importance == NotificationManager.IMPORTANCE_NONE ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getNotificationChannelGroup(group)?.isBlocked == true)
        } else false
    } ?: false

    fun toNotifStatus(context: Context) = with(NotificationManagerCompat.from(context)) {
        if (areNotificationsEnabled()) NotifStatus(
            listOf(
                isDisabled(PushNotificationsGroup.UpdateNotifChannelId),
                isDisabled(PushNotificationsGroup.NewsNotifChannelId),
                isDisabled(DownloadAndInstallationGroup.DownloadStatusNotifChannelId),
            )
        ) else NotifStatus()
    }

    /**
     * First deletes old channels, then creates current groups & their corresponding channels.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshNotificationChannels(context: Context) = with(NotificationManagerCompat.from(context)) {
        deleteOldNotificationChannels()
        createAllNotificationGroups(context)
        createAllNotificationChannels(context)
    }

    /** Deletes all old notification channels */
    @Suppress("DEPRECATION", "NOTHING_TO_INLINE")
    @RequiresApi(Build.VERSION_CODES.O)
    private inline fun NotificationManagerCompat.deleteOldNotificationChannels() {
        deleteNotificationChannel(NotificationChannels.OldPushNotifChannelId)
        deleteNotificationChannel(NotificationChannels.OldProgressNotifChannelId)
        // No longer used in v5.9.0+
        deleteNotificationChannel(MiscellaneousGroup.OtaFilenameSubmittedNotifChannelId)
    }

    /**
     * Creates all notification channel groups
     *
     * @see NotificationChannels
     */
    @Suppress("NOTHING_TO_INLINE")
    @RequiresApi(Build.VERSION_CODES.O)
    private inline fun NotificationManagerCompat.createAllNotificationGroups(context: Context) = createNotificationChannelGroups(
        listOf(
            NotificationChannelGroup(
                DownloadAndInstallationGroup.ID,
                context.getString(R.string.download_and_installation_notifications_group_name)
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    description = context.getString(R.string.download_and_installation_notifications_group_description)
                }
            },
            NotificationChannelGroup(
                PushNotificationsGroup.ID,
                context.getString(R.string.push_notifications_group_name)
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    description = context.getString(R.string.push_notifications_group_description)
                }
            },
            NotificationChannelGroup(
                MiscellaneousGroup.ID,
                context.getString(R.string.miscellaneous_notifications_group_name)
            )
        )
    )

    /**
     * Creates all notification channels and assigns them to their groups
     *
     * @see DownloadAndInstallationGroup
     * @see PushNotificationsGroup
     * @see MiscellaneousGroup
     */
    @Suppress("NOTHING_TO_INLINE")
    @RequiresApi(Build.VERSION_CODES.O)
    private inline fun NotificationManagerCompat.createAllNotificationChannels(context: Context) = createNotificationChannels(
        listOf(
            //// BEGIN: DownloadAndInstallationGroup
            createNotificationChannel(
                DownloadAndInstallationGroup.DownloadStatusNotifChannelId,
                DownloadAndInstallationGroup.ID,
                context.getString(R.string.download_status_notification_channel_name),
                context.getString(R.string.download_status_notification_channel_description),
                NotificationManager.IMPORTANCE_LOW,
                lightsEnabled = false
            ),
            createNotificationChannel(
                DownloadAndInstallationGroup.VerificationStatusNotifChannelId,
                DownloadAndInstallationGroup.ID,
                context.getString(R.string.verification_status_notification_channel_name),
                context.getString(R.string.verification_status_notification_channel_description),
                NotificationManager.IMPORTANCE_LOW,
                lightsEnabled = false
            ),
            createNotificationChannel(
                DownloadAndInstallationGroup.InstallationStatusNotifChannelId,
                DownloadAndInstallationGroup.ID,
                context.getString(R.string.installation_status_notification_channel_name),
                context.getString(R.string.installation_status_notification_channel_description),
                NotificationManager.IMPORTANCE_LOW,
                lightsEnabled = false
            ),
            //// END: DownloadAndInstallationGroup

            //// BEGIN: PushNotificationsGroup
            createNotificationChannel(
                PushNotificationsGroup.UpdateNotifChannelId,
                PushNotificationsGroup.ID,
                context.getString(R.string.update_notification_channel_name),
                context.getString(R.string.update_notification_channel_description),
                NotificationManager.IMPORTANCE_HIGH,
                vibrationEnabled = true
            ),
            createNotificationChannel(
                PushNotificationsGroup.NewsNotifChannelId,
                PushNotificationsGroup.ID,
                context.getString(R.string.news_notification_channel_name),
                context.getString(R.string.news_notification_channel_description),
                NotificationManager.IMPORTANCE_HIGH,
                vibrationEnabled = true
            ),
            createNotificationChannel(
                PushNotificationsGroup.DeviceNotifChannelId,
                PushNotificationsGroup.ID,
                context.getString(R.string.device_notification_channel_name),
                context.getString(R.string.device_notification_channel_description),
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            createNotificationChannel(
                PushNotificationsGroup.GeneralNotifChannelId,
                PushNotificationsGroup.ID,
                context.getString(R.string.general_notification_channel_name),
                context.getString(R.string.general_notification_channel_description),
                NotificationManager.IMPORTANCE_DEFAULT,
                vibrationEnabled = true
            ),
            //// END: PushNotificationsGroup

            //// BEGIN: MiscellaneousGroup
            createNotificationChannel(
                MiscellaneousGroup.OtaUrlSubmittedNotifChannelId,
                MiscellaneousGroup.ID,
                context.getString(R.string.url_submitted_notification_channel_name),
                context.getString(R.string.url_submitted_notification_channel_description),
                NotificationManager.IMPORTANCE_LOW
            ),
            //// END: MiscellaneousGroup
        )
    )

    /**
     * Utility function to configure and create a notification channel.
     *
     * @param lightsEnabled defaults to true
     * @param vibrationEnabled defaults to false
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelId: String,
        groupId: String,
        channelName: String,
        channelDescription: String,
        importance: Int,
        lightsEnabled: Boolean = true,
        vibrationEnabled: Boolean = false,
    ) = NotificationChannel(channelId, channelName, importance).apply {
        group = groupId
        description = channelDescription

        // Force-disable sound for LOW & MIN importance notifications
        if (importance < NotificationManager.IMPORTANCE_DEFAULT) setSound(null, null)

        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        lightColor = Color.RED
        enableLights(lightsEnabled)
        enableVibration(vibrationEnabled)
    }
}

/** @param disabled if null or empty, it's assumed notifications are disabled as a whole */
@Immutable
data class NotifStatus(@Size(3) val disabled: List<Boolean>? = null)

/**
 * Generally, the format is:
 * * Remote: Multiples of 1000000
 * * Local: Multiples of 10
 * * Foreground: Multiples of 100
 *
 * Intermediate values (e.g. 11, 102, 1005, etc) can be used to further differentiate between the same category
 */
object NotificationIds {
    private const val localIdMultiplier = 10
    private const val remoteIdMultiplier = 1000000

    const val LocalDownload = 1 * localIdMultiplier
    const val LocalDownloadForeground = 10 * localIdMultiplier

    const val LocalMd5Verification = 2 * localIdMultiplier
    const val LocalContribution = 3 * localIdMultiplier
    const val LocalInstallationStatus = 4 * localIdMultiplier

    const val RemoteNewUpdate = 1 * remoteIdMultiplier

    const val RemoteNewDevice = 2 * remoteIdMultiplier
    const val RemoteNewDeviceGroup = 20 * remoteIdMultiplier

    const val RemoteNews = 3 * remoteIdMultiplier
    const val RemoteNewsGroup = 30 * remoteIdMultiplier

    const val RemoteGeneral = 4 * remoteIdMultiplier
    const val RemoteGeneralGroup = 40 * remoteIdMultiplier

    const val RemoteUnknown = 5 * remoteIdMultiplier
    const val RemoteUnknownGroup = 50 * remoteIdMultiplier
}

/**
 * Prepare for better, fine-grained notification channels
 */
object NotificationChannels {

    private const val prefix = "com.oxygenupdater.notifications"
    private const val groupPrefix = "$prefix.group"
    private const val channelPrefix = "$prefix.channel"

    object DownloadAndInstallationGroup {

        /**
         * This group's ID
         */
        const val ID = "$groupPrefix.download&installation"

        /**
         * Name: Download status
         * Description: Notifications that reflect the current state of a download: pending, active, completed, or failed
         */
        const val DownloadStatusNotifChannelId = "$channelPrefix.download"

        /**
         * Name: Verification status
         * Description: Notifications that reflect the current state of a file verification task: in-progress, completed, or failed.
         * MD5 checksums for downloaded files are verified to ensure the file hasn't been tampered with, and is a valid update file.
         */
        const val VerificationStatusNotifChannelId = "$channelPrefix.verification"

        /**
         * Name: Installation status
         * Description: Get notified if an update was successfully installed or not
         */
        const val InstallationStatusNotifChannelId = "$channelPrefix.installation"
    }

    object PushNotificationsGroup {

        /**
         * This group's ID
         */
        const val ID = "$groupPrefix.push"

        /**
         * Name: System update available
         * Description: Get notified when there's a new update available for your selected device & update method
         */
        const val UpdateNotifChannelId = "$channelPrefix.update"

        /**
         * Name: News article published
         * Description: Get notified when we publish a news article
         */
        const val NewsNotifChannelId = "$channelPrefix.news"

        /**
         * Name: New device supported
         * Description: Get notified when the app adds support for a new device
         */
        const val DeviceNotifChannelId = "$channelPrefix.device"

        /**
         * Name: General
         * Description: General notifications that don't fit into other categories
         */
        const val GeneralNotifChannelId = "$channelPrefix.general"
    }

    object MiscellaneousGroup {

        /**
         * This group's ID
         */
        const val ID = "$groupPrefix.miscellaneous"

        /**
         * Name: OTA filename submitted
         * Description: Get notified when you've submitted OTA filenames successfully to our team.
         * You can opt-out of automatically submitting filenames in the app's settings.
         */
        @Deprecated(
            message = """
                No longer used in v5.9.0+, because all OnePlus phones now use either
                Google OTA (doesn't save files anywhere), or Component OTA (saves in
                a directory accessible only via root), but URL in this case can't be
                constructed via filenames alone.
            """,
            level = DeprecationLevel.WARNING
        )
        const val OtaFilenameSubmittedNotifChannelId = "$channelPrefix.filename"

        const val OtaUrlSubmittedNotifChannelId = "$channelPrefix.url"
    }

    /**
     * **Note: don't ever change this string's value**
     */
    @Deprecated(
        message = """
            No longer used in v5.0.0+ (redesign release), except for deleting
            old channels. Was used for push notifications (general, new device
            supported, new update available, news article published), as well
            as to display installation status notifications on rooted devices.
        """,
        level = DeprecationLevel.WARNING
    )
    const val OldPushNotifChannelId = "com.oxygenupdater.internal.notifications"

    /**
     * **Note: don't ever change this string's value**
     */
    @Deprecated(
        message = """
            No longer used in v5.0.0+ (redesign release), except for deleting
            old channels. Was used for the entire download flow, as well as
            successful OTA filename contribution notifications.
        """,
        level = DeprecationLevel.WARNING
    )
    const val OldProgressNotifChannelId = "com.oxygenupdater.progress"
}
