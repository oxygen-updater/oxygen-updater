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

    const val FOREGROUND_NOTIFICATION_DOWNLOAD = 1000
    const val FOREGROUND_NOTIFICATION_CONTRIBUTION = 2000
}
