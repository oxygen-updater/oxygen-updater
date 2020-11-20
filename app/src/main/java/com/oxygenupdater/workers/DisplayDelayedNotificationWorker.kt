package com.oxygenupdater.workers

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oxygenupdater.R
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.activities.NewsItemActivity
import com.oxygenupdater.database.LocalAppDb
import com.oxygenupdater.enums.NotificationElement
import com.oxygenupdater.enums.NotificationType
import com.oxygenupdater.exceptions.OxygenUpdaterException
import com.oxygenupdater.extensions.setBigTextStyle
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.AppLocale
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.DEVICE_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.GENERAL_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.NEWS_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.UPDATE_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationIds
import org.koin.java.KoinJavaComponent.inject

/**
 * Enqueued from [com.oxygenupdater.services.FirebaseMessagingService]
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

    private val localAppDb by inject(LocalAppDb::class.java)
    private val notificationManager by inject(NotificationManagerCompat::class.java)

    private val newsItemDao by lazy {
        localAppDb.newsItemDao()
    }

    override suspend fun doWork(): Result {
        if (messageContents.isNullOrEmpty()) {
            return Result.failure()
        }

        val notificationType = NotificationType.valueOf(
            messageContents[NotificationElement.TYPE.name] ?: ""
        )

        val builder = when (notificationType) {
            NotificationType.NEW_DEVICE -> if (!SettingsManager.getPreference(
                    SettingsManager.PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS,
                    true
                )
            ) {
                return Result.success()
            } else {
                getNewDeviceNotificationBuilder(messageContents[NotificationElement.NEW_DEVICE_NAME.name])
            }
            NotificationType.NEW_VERSION -> if (!SettingsManager.getPreference(
                    SettingsManager.PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS,
                    true
                )
            ) {
                return Result.success()
            } else {
                getNewVersionNotificationBuilder(
                    messageContents[NotificationElement.DEVICE_NAME.name],
                    messageContents[NotificationElement.NEW_VERSION_NUMBER.name]
                )
            }
            NotificationType.GENERAL_NOTIFICATION -> if (!SettingsManager.getPreference(
                    SettingsManager.PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS,
                    true
                )
            ) {
                // Don't show notification if user has opted out
                return Result.success()
            } else {
                getGeneralNotificationBuilder(
                    if (AppLocale.get() == AppLocale.NL) {
                        messageContents[NotificationElement.DUTCH_MESSAGE.name]
                    } else {
                        messageContents[NotificationElement.ENGLISH_MESSAGE.name]
                    }
                )
            }
            NotificationType.NEWS -> if (!SettingsManager.getPreference(
                    SettingsManager.PROPERTY_RECEIVE_NEWS_NOTIFICATIONS,
                    true
                )
            ) {
                return Result.success()
            } else {
                // If this is a "dump" notification, show it only to people who haven't yet read the article
                // A "bump" is defined as re-sending the notification so that people who haven't yet read the article can read it
                // However, only app versions from v4.1.0 onwards properly support this,
                // even though a broken implementation was added in v4.0.0 (Kotlin rebuild).
                // So use the "bump" feature on admin portal with care - the notification will still be shown on older app versions
                if (messageContents[NotificationElement.NEWS_ITEM_IS_BUMP.name]?.toBoolean() == true
                    && newsItemDao.getById(
                        messageContents[NotificationElement.NEWS_ITEM_ID.name]?.toLong()
                    )?.read == true
                ) {
                    return Result.success()
                }

                getNewsArticleNotificationBuilder(
                    if (AppLocale.get() == AppLocale.NL) {
                        messageContents[NotificationElement.DUTCH_MESSAGE.name]
                    } else {
                        messageContents[NotificationElement.ENGLISH_MESSAGE.name]
                    }
                )
            }
        }

        if (builder == null) {
            logError(
                TAG,
                OxygenUpdaterException("Failed to instantiate notificationBuilder. Can not display push notification!")
            )
            return Result.failure()
        }

        builder.setSmallIcon(R.drawable.logo_notification)
            .setContentIntent(getNotificationIntent(notificationType))
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(
            getNotificationId(notificationType),
            builder.build()
        )

        return Result.success()
    }

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    private fun getNotificationId(type: NotificationType) = when (type) {
        NotificationType.NEW_VERSION -> NotificationIds.REMOTE_NOTIFICATION_NEW_UPDATE
        NotificationType.NEWS -> NotificationIds.REMOTE_NOTIFICATION_NEWS
        NotificationType.NEW_DEVICE -> NotificationIds.REMOTE_NOTIFICATION_NEW_DEVICE
        NotificationType.GENERAL_NOTIFICATION -> NotificationIds.REMOTE_NOTIFICATION_GENERIC
        else -> NotificationIds.REMOTE_NOTIFICATION_UNKNOWN
    }

    private fun getNewVersionNotificationBuilder(
        deviceName: String?,
        versionNumber: String?
    ) = NotificationCompat.Builder(context, UPDATE_NOTIFICATION_CHANNEL_ID)
        .setPriority(PRIORITY_HIGH)
        .setContentTitle(context.getString(R.string.update_notification_channel_name))
        .setBigTextStyle(
            context.getString(
                R.string.notification_version,
                versionNumber,
                deviceName ?: context.getString(R.string.device_information_unknown)
            )
        )

    private fun getNewsArticleNotificationBuilder(
        message: String?
    ) = NotificationCompat.Builder(context, NEWS_NOTIFICATION_CHANNEL_ID)
        .setPriority(PRIORITY_HIGH)
        .setContentTitle(context.getString(R.string.news_notification_channel_name))
        .setBigTextStyle(message)

    private fun getNewDeviceNotificationBuilder(
        newDeviceName: String?
    ) = NotificationCompat.Builder(context, DEVICE_NOTIFICATION_CHANNEL_ID)
        .setPriority(PRIORITY_DEFAULT)
        .setContentTitle(context.getString(R.string.device_notification_channel_name))
        .setBigTextStyle(
            context.getString(
                R.string.notification_new_device_text,
                newDeviceName ?: context.getString(R.string.device_information_unknown)
            )
        )

    private fun getGeneralNotificationBuilder(
        message: String?
    ) = NotificationCompat.Builder(context, GENERAL_NOTIFICATION_CHANNEL_ID)
        .setPriority(PRIORITY_DEFAULT)
        .setContentTitle(context.getString(R.string.general_notification_channel_name))
        .setBigTextStyle(message)

    private fun getNotificationIntent(
        notificationType: NotificationType
    ) = PendingIntent.getActivity(
        context,
        0,
        if (notificationType == NotificationType.NEWS) {
            Intent(context, NewsItemActivity::class.java)
                .putExtra(
                    NewsItemActivity.INTENT_NEWS_ITEM_ID,
                    messageContents[NotificationElement.NEWS_ITEM_ID.name]?.toLong()
                )
                .putExtra(NewsItemActivity.INTENT_DELAY_AD_START, true)
        } else {
            Intent(context, MainActivity::class.java)
        },
        FLAG_UPDATE_CURRENT
    )

    companion object {
        private const val TAG = "DisplayDelayedNotificationWorker"
    }
}
