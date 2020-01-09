package com.arjanvlek.oxygenupdater.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.i18n.Locale
import com.arjanvlek.oxygenupdater.internal.i18n.Locale.NL
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.news.NewsActivity
import com.arjanvlek.oxygenupdater.notifications.NotificationElement.DEVICE_NAME
import com.arjanvlek.oxygenupdater.notifications.NotificationElement.DUTCH_MESSAGE
import com.arjanvlek.oxygenupdater.notifications.NotificationElement.ENGLISH_MESSAGE
import com.arjanvlek.oxygenupdater.notifications.NotificationElement.NEW_DEVICE_NAME
import com.arjanvlek.oxygenupdater.notifications.NotificationElement.NEW_VERSION_NUMBER
import com.arjanvlek.oxygenupdater.notifications.NotificationElement.TYPE
import com.arjanvlek.oxygenupdater.notifications.NotificationType.GENERAL_NOTIFICATION
import com.arjanvlek.oxygenupdater.notifications.NotificationType.NEWS
import com.arjanvlek.oxygenupdater.notifications.NotificationType.NEW_DEVICE
import com.arjanvlek.oxygenupdater.notifications.NotificationType.NEW_VERSION
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.MainActivity
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException

/**
 * Oxygen Updater - © 2018 Arjan Vlek
 */
class DelayedPushNotificationDisplayer : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        if (params == null || !params.extras.containsKey(KEY_NOTIFICATION_CONTENTS) || application !is ApplicationData) {
            return exit(params, false)
        }

        val settingsManager = SettingsManager(application)

        // Get notification contents of FCM.
        val notificationContentsTypeRef: TypeReference<Map<String, String>> = object : TypeReference<Map<String, String>>() {}
        val messageContents: Map<String, String>
        val notificationContentsJson = params.extras.getString(KEY_NOTIFICATION_CONTENTS)

        messageContents = try {
            jacksonObjectMapper().readValue(notificationContentsJson, notificationContentsTypeRef)
        } catch (e: IOException) {
            logError(TAG, OxygenUpdaterException("Failed to read notification contents from JSON string ($notificationContentsJson)"))
            return exit(params, false)
        }

        val notificationType = NotificationType.valueOf(messageContents[TYPE.toString()] ?: "")

        val builder: NotificationCompat.Builder? = when (notificationType) {
            NEW_DEVICE -> {
                if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS, true)) {
                    return exit(params, true)
                }

                getBuilderForNewDeviceNotification(messageContents[NEW_DEVICE_NAME.toString()])
            }
            NEW_VERSION -> {
                if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS, true)) {
                    return exit(params, true)
                }

                getBuilderForNewVersionNotification(messageContents[DEVICE_NAME.toString()], messageContents[NEW_VERSION_NUMBER.toString()])
            }
            GENERAL_NOTIFICATION -> {
                if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS, true)) {
                    return exit(params, true)
                }

                val message = if (Locale.locale == NL) messageContents[DUTCH_MESSAGE.toString()] else messageContents[ENGLISH_MESSAGE.toString()]

                getBuilderForGeneralServerNotificationOrNewsNotification(message)
            }
            NEWS -> {
                if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_NEWS_NOTIFICATIONS, true)) {
                    return exit(params, true)
                }

                val newsMessage = if (Locale.locale == NL) messageContents[DUTCH_MESSAGE.toString()] else messageContents[ENGLISH_MESSAGE.toString()]

                getBuilderForGeneralServerNotificationOrNewsNotification(newsMessage)
            }
        }

        if (builder == null) {
            logError(TAG, OxygenUpdaterException("Failed to instantiate notificationBuilder. Can not display push notification!"))
            return exit(params, false)
        }

        builder.setContentIntent(getNotificationIntent(notificationType, messageContents))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)

        val notificationId = getNotificationId(notificationType)
        val notification = builder.build()
        val notificationManager: NotificationManager? = Utils.getSystemService(this, Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager == null) {
            logError(TAG, OxygenUpdaterException("Notification Manager service is not available"))
            return exit(params, false)
        }

        notificationManager.notify(notificationId, notification)

        return exit(params, true)
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    private fun getNotificationId(type: NotificationType): Int {
        return when (type) {
            NEW_DEVICE -> NEW_DEVICE_NOTIFICATION_ID
            NEW_VERSION -> NEW_UPDATE_NOTIFICATION_ID
            GENERAL_NOTIFICATION -> GENERIC_NOTIFICATION_ID
            NEWS -> NEWS_NOTIFICATION_ID
            else -> UNKNOWN_NOTIFICATION_ID
        }
    }

    private fun getBuilderForGeneralServerNotificationOrNewsNotification(message: String?): NotificationCompat.Builder {
        return notificationBuilder
            .setSmallIcon(R.drawable.oxygen_updater)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
    }

    private fun getBuilderForNewDeviceNotification(newDeviceName: String?): NotificationCompat.Builder {
        val message = getString(R.string.notification_new_device, newDeviceName)

        return notificationBuilder
            .setSmallIcon(R.drawable.new_text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message).setSummaryText(getString(R.string.notification_new_device_short)))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
    }

    private fun getBuilderForNewVersionNotification(deviceName: String?, versionNumber: String?): NotificationCompat.Builder {
        val message = getString(R.string.notification_version, versionNumber, deviceName)

        return notificationBuilder
            .setSmallIcon(R.drawable.new_text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setWhen(System.currentTimeMillis())
            .setContentTitle(getString(R.string.notification_version_title))
            .setContentText(message)
    }

    private val notificationBuilder: NotificationCompat.Builder
        get() = if (Build.VERSION.SDK_INT >= 26) {
            NotificationCompat.Builder(this, ApplicationData.PUSH_NOTIFICATION_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }

    private fun getNotificationIntent(notificationType: NotificationType, messageContents: Map<String, String>): PendingIntent {
        val contentIntent: PendingIntent
        contentIntent = if (notificationType == NEWS) {
            val newsIntent = Intent(this, NewsActivity::class.java)
                .putExtra(NewsActivity.INTENT_NEWS_ITEM_ID, messageContents[NotificationElement.NEWS_ITEM_ID.toString()]?.toLong())
                .putExtra(NewsActivity.INTENT_START_WITH_AD, true)

            PendingIntent.getActivity(this, 0, newsIntent, 0)
        } else {
            PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)
        }
        return contentIntent
    }

    private fun exit(parameters: JobParameters?, success: Boolean): Boolean {
        jobFinished(parameters, !success)
        return success
    }

    companion object {
        const val NEW_DEVICE_NOTIFICATION_ID = 10010
        const val NEW_UPDATE_NOTIFICATION_ID = 20020
        const val GENERIC_NOTIFICATION_ID = 30030
        const val NEWS_NOTIFICATION_ID = 50050
        const val UNKNOWN_NOTIFICATION_ID = 40040
        const val KEY_NOTIFICATION_CONTENTS = "notification-contents"
        private const val TAG = "DelayedPushNotificationDisplayer"
    }
}
