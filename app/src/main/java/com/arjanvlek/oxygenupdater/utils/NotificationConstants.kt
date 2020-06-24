package com.arjanvlek.oxygenupdater.utils

/**
 * Generally, the format is:
 * * Remote: Multiples of 10
 * * Local: Multiples of 100
 * * Foreground: Multiples of 1000
 *
 * Intermediate values (e.g. 11, 102, 1005, etc) can be used to further differentiate between the same category
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */

object NotificationIds {
    const val REMOTE_NOTIFICATION_NEW_DEVICE = 10
    const val REMOTE_NOTIFICATION_NEW_UPDATE = 20
    const val REMOTE_NOTIFICATION_NEWS = 30
    const val REMOTE_NOTIFICATION_GENERIC = 40
    const val REMOTE_NOTIFICATION_UNKNOWN = 50

    const val LOCAL_NOTIFICATION_DOWNLOAD = 100
    const val LOCAL_NOTIFICATION_MD5_VERIFICATION = 200
    const val LOCAL_NOTIFICATION_CONTRIBUTION = 300
    const val LOCAL_NOTIFICATION_INSTALLATION_STATUS = 400

    const val FOREGROUND_NOTIFICATION_DOWNLOAD = 1000
}

/**
 * Prepare for better, fine-grained notification channels
 */
object NotificationChannels {

    object DownloadAndInstallationGroup {
        /**
         * Name: Download status
         * Description: Notifications that reflect the current state of a download: pending, active, completed, or failed
         */
        const val DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications.download"

        /**
         * Name: Verification status
         * Description: Notifications that reflect the current state of a file verification task: in-progress, completed, or failed.
         * MD5 checksums for downloaded files are verified to ensure the file hasn't been tampered with, and is a valid update file.
         */
        const val VERIFICATION_STATUS_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications.verification"

        /**
         * Name: Installation status
         * Description: Get notified if an update was successfully installed or not
         */
        const val INSTALLATION_STATUS_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications.installation"
    }

    object PushNotificationsGroup {
        /**
         * Name: System update available
         * Description: Get notified when there's a new update available for your selected device & update method
         */
        const val UPDATE_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications.update"

        /**
         * Name: News article published
         * Description: Get notified when we publish a news article
         */
        const val NEWS_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications.news"

        /**
         * Name: New device supported
         * Description: Get notified when the app adds support for a new device
         */
        const val DEVICE_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications.device"

        /**
         * Name: General
         * Description: General notifications that don't fit into other categories
         */
        const val GENERAL_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications.general"
    }

    object MiscellaneousGroup {
        /**
         * Name: OTA filename submitted
         * Description: Get notified when you've submitted OTA filenames successfully to our team (you can opt-out of automatically submitting filenames in the app's settings)
         */
        const val OTA_FILENAME_SUBMITTED_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications.filename"
    }

    /**
     * Was used for push notifications (general, new device supported, new update available, news article published),
     * as well as to display installation successful/failed notifications (only on rooted devices).
     */
    const val OLD_PUSH_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.internal.notifications"

    /**
     * Was used for the entire download flow, as well as successful filename contribution
     */
    const val OLD_PROGRESS_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.progress"
}
