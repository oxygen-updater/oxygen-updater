package com.oxygenupdater.utils

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.oxygenupdater.R
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup
import com.oxygenupdater.utils.NotificationChannels.MiscellaneousGroup
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup
import org.koin.java.KoinJavaComponent.getKoin

private val notificationManager by getKoin().inject<NotificationManagerCompat>()

class NotificationUtils(private val context: Context) {

    /**
     * Deletes all old notification channels
     */
    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteOldNotificationChannels() {
        notificationManager.deleteNotificationChannel(
            NotificationChannels.OLD_PUSH_NOTIFICATION_CHANNEL_ID
        )
        notificationManager.deleteNotificationChannel(
            NotificationChannels.OLD_PROGRESS_NOTIFICATION_CHANNEL_ID
        )
    }

    /**
     * Creates all notification channel groups, as well as their channels
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createNewNotificationGroupsAndChannels() = createAllNotificationGroups().also {
        createAllNotificationChannels()
    }

    /**
     * Creates all notification channel groups
     *
     * @see NotificationChannels
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAllNotificationGroups() = notificationManager.createNotificationChannelGroups(
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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAllNotificationChannels() {
        notificationManager.createNotificationChannels(
            listOf(
                //// BEGIN: DownloadAndInstallationGroup
                createNotificationChannel(
                    DownloadAndInstallationGroup.DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID,
                    DownloadAndInstallationGroup.ID,
                    context.getString(R.string.download_status_notification_channel_name),
                    context.getString(R.string.download_status_notification_channel_description),
                    NotificationManager.IMPORTANCE_LOW,
                    lightsEnabled = false
                ),
                createNotificationChannel(
                    DownloadAndInstallationGroup.VERIFICATION_STATUS_NOTIFICATION_CHANNEL_ID,
                    DownloadAndInstallationGroup.ID,
                    context.getString(R.string.verification_status_notification_channel_name),
                    context.getString(R.string.verification_status_notification_channel_description),
                    NotificationManager.IMPORTANCE_LOW,
                    lightsEnabled = false
                ),
                createNotificationChannel(
                    DownloadAndInstallationGroup.INSTALLATION_STATUS_NOTIFICATION_CHANNEL_ID,
                    DownloadAndInstallationGroup.ID,
                    context.getString(R.string.installation_status_notification_channel_name),
                    context.getString(R.string.installation_status_notification_channel_description),
                    NotificationManager.IMPORTANCE_LOW,
                    lightsEnabled = false
                ),
                //// END: DownloadAndInstallationGroup

                //// BEGIN: PushNotificationsGroup
                createNotificationChannel(
                    PushNotificationsGroup.UPDATE_NOTIFICATION_CHANNEL_ID,
                    PushNotificationsGroup.ID,
                    context.getString(R.string.update_notification_channel_name),
                    context.getString(R.string.update_notification_channel_description),
                    NotificationManager.IMPORTANCE_HIGH,
                    vibrationEnabled = true
                ),
                createNotificationChannel(
                    PushNotificationsGroup.NEWS_NOTIFICATION_CHANNEL_ID,
                    PushNotificationsGroup.ID,
                    context.getString(R.string.news_notification_channel_name),
                    context.getString(R.string.news_notification_channel_description),
                    NotificationManager.IMPORTANCE_HIGH,
                    vibrationEnabled = true
                ),
                createNotificationChannel(
                    PushNotificationsGroup.DEVICE_NOTIFICATION_CHANNEL_ID,
                    PushNotificationsGroup.ID,
                    context.getString(R.string.device_notification_channel_name),
                    context.getString(R.string.device_notification_channel_description),
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                createNotificationChannel(
                    PushNotificationsGroup.GENERAL_NOTIFICATION_CHANNEL_ID,
                    PushNotificationsGroup.ID,
                    context.getString(R.string.general_notification_channel_name),
                    context.getString(R.string.general_notification_channel_description),
                    NotificationManager.IMPORTANCE_DEFAULT,
                    vibrationEnabled = true
                ),
                //// END: PushNotificationsGroup

                //// BEGIN: MiscellaneousGroup
                createNotificationChannel(
                    MiscellaneousGroup.OTA_FILENAME_SUBMITTED_NOTIFICATION_CHANNEL_ID,
                    MiscellaneousGroup.ID,
                    context.getString(R.string.filename_submitted_notification_channel_name),
                    context.getString(R.string.filename_submitted_notification_channel_description),
                    NotificationManager.IMPORTANCE_LOW
                ),
                //// END: MiscellaneousGroup
            )
        )
    }

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
        vibrationEnabled: Boolean = false
    ) = NotificationChannel(
        channelId,
        channelName,
        importance
    ).apply {
        group = groupId
        description = channelDescription

        // Force-disable sound for LOW & MIN importance notifications
        if (importance < NotificationManager.IMPORTANCE_DEFAULT) {
            setSound(null, null)
        }

        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        lightColor = Color.RED
        enableLights(lightsEnabled)
        enableVibration(vibrationEnabled)
    }
}

/**
 * Generally, the format is:
 * * Remote: Multiples of 1000000
 * * Local: Multiples of 10
 * * Foreground: Multiples of 100
 *
 * Intermediate values (e.g. 11, 102, 1005, etc) can be used to further differentiate between the same category
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
object NotificationIds {
    private const val localIdMultiplier = 10
    private const val remoteIdMultiplier = 1000000

    const val LOCAL_DOWNLOAD = 1 * localIdMultiplier
    const val LOCAL_DOWNLOAD_FOREGROUND = 10 * localIdMultiplier

    const val LOCAL_MD5_VERIFICATION = 2 * localIdMultiplier
    const val LOCAL_CONTRIBUTION = 3 * localIdMultiplier
    const val LOCAL_INSTALLATION_STATUS = 4 * localIdMultiplier

    const val REMOTE_NEW_UPDATE = 1 * remoteIdMultiplier

    const val REMOTE_NEW_DEVICE = 2 * remoteIdMultiplier
    const val REMOTE_NEW_DEVICE_GROUP = 20 * remoteIdMultiplier

    const val REMOTE_NEWS = 3 * remoteIdMultiplier
    const val REMOTE_NEWS_GROUP = 30 * remoteIdMultiplier

    const val REMOTE_GENERAL = 4 * remoteIdMultiplier
    const val REMOTE_GENERAL_GROUP = 40 * remoteIdMultiplier

    const val REMOTE_UNKNOWN = 5 * remoteIdMultiplier
    const val REMOTE_UNKNOWN_GROUP = 50 * remoteIdMultiplier
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
        const val DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID = "$channelPrefix.download"

        /**
         * Name: Verification status
         * Description: Notifications that reflect the current state of a file verification task: in-progress, completed, or failed.
         * MD5 checksums for downloaded files are verified to ensure the file hasn't been tampered with, and is a valid update file.
         */
        const val VERIFICATION_STATUS_NOTIFICATION_CHANNEL_ID = "$channelPrefix.verification"

        /**
         * Name: Installation status
         * Description: Get notified if an update was successfully installed or not
         */
        const val INSTALLATION_STATUS_NOTIFICATION_CHANNEL_ID = "$channelPrefix.installation"
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
        const val UPDATE_NOTIFICATION_CHANNEL_ID = "$channelPrefix.update"

        /**
         * Name: News article published
         * Description: Get notified when we publish a news article
         */
        const val NEWS_NOTIFICATION_CHANNEL_ID = "$channelPrefix.news"

        /**
         * Name: New device supported
         * Description: Get notified when the app adds support for a new device
         */
        const val DEVICE_NOTIFICATION_CHANNEL_ID = "$channelPrefix.device"

        /**
         * Name: General
         * Description: General notifications that don't fit into other categories
         */
        const val GENERAL_NOTIFICATION_CHANNEL_ID = "$channelPrefix.general"
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
        const val OTA_FILENAME_SUBMITTED_NOTIFICATION_CHANNEL_ID = "$channelPrefix.filename"
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
    const val OLD_PUSH_NOTIFICATION_CHANNEL_ID = "com.oxygenupdater.internal.notifications"

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
    const val OLD_PROGRESS_NOTIFICATION_CHANNEL_ID = "com.oxygenupdater.progress"
}
