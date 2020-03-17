package com.arjanvlek.oxygenupdater.workers

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.activities.MainActivity
import com.arjanvlek.oxygenupdater.activities.NewsActivity
import com.arjanvlek.oxygenupdater.enums.NotificationElement
import com.arjanvlek.oxygenupdater.enums.NotificationType
import com.arjanvlek.oxygenupdater.exceptions.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.AppLocale
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.NotificationIds
import org.koin.java.KoinJavaComponent.inject

/**
 * Enqueued from [com.arjanvlek.oxygenupdater.services.FirebaseMessagingService]
 * to display a notification to the user after a specified delay
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class DisplayDelayedNotificationWorker(
    private val context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val messageContents = parameters.inputData.keyValueMap
        .entries
        .associate { it.key to it.value.toString() }

    private val notificationBuilder = NotificationCompat.Builder(
        context,
        OxygenUpdater.PUSH_NOTIFICATION_CHANNEL_ID
    )

    private val notificationManager by inject(NotificationManager::class.java)
    private val settingsManager by inject(SettingsManager::class.java)

    override suspend fun doWork(): Result {
        if (messageContents.isNullOrEmpty()) {
            return Result.failure()
        }

        val notificationType = NotificationType.valueOf(
            messageContents[NotificationElement.TYPE.name] ?: ""
        )

        val builder = when (notificationType) {
            NotificationType.NEW_DEVICE -> if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS, true)) {
                return Result.success()
            } else {
                getNewDeviceNotificationBuilder(messageContents[NotificationElement.NEW_DEVICE_NAME.name])
            }
            NotificationType.NEW_VERSION -> if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS, true)) {
                return Result.success()
            } else {
                getNewVersionNotificationBuilder(
                    messageContents[NotificationElement.DEVICE_NAME.name],
                    messageContents[NotificationElement.NEW_VERSION_NUMBER.name]
                )
            }
            NotificationType.GENERAL_NOTIFICATION -> if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS, true)) {
                return Result.success()
            } else {
                val message = if (AppLocale.get() == AppLocale.NL) {
                    messageContents[NotificationElement.DUTCH_MESSAGE.name]
                } else {
                    messageContents[NotificationElement.ENGLISH_MESSAGE.name]
                }

                getGeneralServerOrNewsNotificationBuilder(message)
            }
            NotificationType.NEWS -> if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_NEWS_NOTIFICATIONS, true)) {
                return Result.success()
            } else {
                val newsMessage = if (AppLocale.get() == AppLocale.NL) {
                    messageContents[NotificationElement.DUTCH_MESSAGE.name]
                } else {
                    messageContents[NotificationElement.ENGLISH_MESSAGE.name]
                }

                getGeneralServerOrNewsNotificationBuilder(newsMessage)
            }
        }

        if (builder == null) {
            logError(
                TAG,
                OxygenUpdaterException("Failed to instantiate notificationBuilder. Can not display push notification!")
            )
            return Result.failure()
        }

        builder.setContentIntent(getNotificationIntent(notificationType))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)

        notificationManager.notify(getNotificationId(notificationType), builder.build())

        return Result.success()
    }

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    private fun getNotificationId(type: NotificationType) = when (type) {
        NotificationType.NEW_DEVICE -> NotificationIds.REMOTE_NOTIFICATION_NEW_DEVICE
        NotificationType.NEW_VERSION -> NotificationIds.REMOTE_NOTIFICATION_NEW_UPDATE
        NotificationType.GENERAL_NOTIFICATION -> NotificationIds.REMOTE_NOTIFICATION_GENERIC
        NotificationType.NEWS -> NotificationIds.REMOTE_NOTIFICATION_NEWS
        else -> NotificationIds.REMOTE_NOTIFICATION_UNKNOWN
    }

    private fun getGeneralServerOrNewsNotificationBuilder(message: String?) = notificationBuilder
        .setSmallIcon(R.drawable.logo_outline)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    private fun getNewDeviceNotificationBuilder(
        newDeviceName: String?
    ) = context.getString(
        R.string.notification_new_device,
        newDeviceName
    ).let {
        notificationBuilder
            .setSmallIcon(R.drawable.new_text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(it).setSummaryText(context.getString(R.string.notification_new_device_short)))
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(it)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    private fun getNewVersionNotificationBuilder(
        deviceName: String?,
        versionNumber: String?
    ) = context.getString(
        R.string.notification_version,
        versionNumber,
        deviceName
    ).let {
        notificationBuilder
            .setSmallIcon(R.drawable.new_text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(it))
            .setWhen(System.currentTimeMillis())
            .setContentTitle(context.getString(R.string.notification_version_title))
            .setContentText(it)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    private fun getNotificationIntent(
        notificationType: NotificationType
    ) = if (notificationType == NotificationType.NEWS) {
        val newsIntent = Intent(context, NewsActivity::class.java)
            .putExtra(NewsActivity.INTENT_NEWS_ITEM_ID, messageContents[NotificationElement.NEWS_ITEM_ID.name]?.toLong())
            .putExtra(NewsActivity.INTENT_DELAY_AD_START, true)

        PendingIntent.getActivity(context, 0, newsIntent, 0)
    } else {
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), 0)
    }

    companion object {
        private const val TAG = "DisplayDelayedNotificationWorker"
    }
}
