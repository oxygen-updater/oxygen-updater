package com.oxygenupdater.workers

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.GROUP_ALERT_CHILDREN
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.imageLoader
import coil.request.ImageRequest
import com.oxygenupdater.R
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.activities.NewsItemActivity
import com.oxygenupdater.database.LocalAppDb
import com.oxygenupdater.enums.NotificationElement
import com.oxygenupdater.enums.NotificationType
import com.oxygenupdater.enums.NotificationType.GENERAL_NOTIFICATION
import com.oxygenupdater.enums.NotificationType.NEWS
import com.oxygenupdater.enums.NotificationType.NEW_DEVICE
import com.oxygenupdater.enums.NotificationType.NEW_VERSION
import com.oxygenupdater.extensions.attachWithLocale
import com.oxygenupdater.extensions.setBigTextStyle
import com.oxygenupdater.models.AppLocale
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.DEVICE_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.GENERAL_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.NEWS_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.UPDATE_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationIds
import com.oxygenupdater.utils.Utils
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.random.Random

/**
 * Enqueued from [com.oxygenupdater.services.FirebaseMessagingService]
 * to display a notification to the user after a specified delay
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class DisplayDelayedNotificationWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    private val context = context.attachWithLocale(false)

    private val messageContents = parameters.inputData.keyValueMap
        .entries
        .associate { it.key to it.value.toString() }

    private val localAppDb by getKoin().inject<LocalAppDb>()
    private val notificationManager by getKoin().inject<NotificationManagerCompat>()

    private val random by lazy(LazyThreadSafetyMode.NONE) {
        Random.Default
    }

    private val newsItemDao by lazy(LazyThreadSafetyMode.NONE) {
        localAppDb.newsItemDao()
    }

    override suspend fun doWork(): Result {
        if (messageContents.isEmpty()) {
            return Result.failure()
        }

        val notificationType = NotificationType.valueOf(
            messageContents[NotificationElement.TYPE.name] ?: ""
        )

        val builder = when (notificationType) {
            NEW_DEVICE -> getNewDeviceNotificationBuilder(messageContents[NotificationElement.NEW_DEVICE_NAME.name])
            NEW_VERSION -> getNewVersionNotificationBuilder(
                messageContents[NotificationElement.DEVICE_NAME.name],
                messageContents[NotificationElement.NEW_VERSION_NUMBER.name]
            )
            GENERAL_NOTIFICATION -> getGeneralNotificationBuilder(
                if (AppLocale.get() == AppLocale.NL) {
                    messageContents[NotificationElement.DUTCH_MESSAGE.name]
                } else {
                    messageContents[NotificationElement.ENGLISH_MESSAGE.name]
                }
            )
            NEWS -> {
                // If this is a "bump" notification, show it only to people who haven't yet read the article
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

        val notificationId = getNotificationId(notificationType)
        val notificationGroupKey = getNotificationGroupKey(notificationType)
        val notificationIntent = getNotificationIntent(
            notificationType,
            notificationId
        )

        builder.setSmallIcon(R.drawable.logo_notification)
            .setContentIntent(notificationIntent)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (notificationType != NEW_VERSION) {
            builder.setGroup(notificationGroupKey)
        }

        notificationManager.notify(notificationId, builder.build())

        val imageUrl = messageContents[NotificationElement.NEWS_ITEM_IMAGE.name]?.trim() ?: ""
        if (imageUrl.isNotEmpty()) {
            // Update an existing notification when image loads (avoids indefinite waits)
            // Note: we're setting only the large icon; not changing style to BigPicture
            //       because that doesn't show full text when expanded like BigText does
            builder.reNotifyWithLargeIcon(notificationId, imageUrl)
        }

        // Summary notification is not shown for API < 24 because notification
        // groups aren't supported anyway (meaning multiple notifications will
        // be shown separately instead of using InboxStyle to emulate a "group").
        if (notificationType != NEW_VERSION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val summaryNotification = NotificationCompat.Builder(
                context,
                notificationGroupKey
            ).setSmallIcon(R.drawable.logo_notification)
                // Dismiss this notification when all its children are also dismissed
                .setAutoCancel(true)
                // Specify which group this notification belongs to
                .setGroup(notificationGroupKey)
                // Set this notification as the summary for the group
                .setGroupSummary(true)
                // Mute this summary notification in favour of children notifications
                .setGroupAlertBehavior(GROUP_ALERT_CHILDREN)
                .build()

            notificationManager.notify(
                getNotificationGroupId(notificationType),
                summaryNotification
            )
        }

        return Result.success()
    }

    private fun NotificationCompat.Builder.reNotifyWithLargeIcon(
        notificationId: Int,
        imageUrl: String,
    ) = context.imageLoader.enqueue(
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Utils.dpToPx(context, 64f).toInt()) // memory optimization
            .target {
                notificationManager.notify(
                    notificationId,
                    setLargeIcon(it.toBitmap()).build()
                )
            }.build()
    )

    /**
     * Generates notification IDs from predefined [NotificationIds], but also
     * guarantees uniqueness for each type that may show multiple notifications:
     *
     * - [NEWS]: add the news item ID to guarantee a unique notification ID
     * - [NEW_DEVICE] & [GENERAL_NOTIFICATION]: Since there isn't any ID for
     *   these notifications, rely on [Random.nextInt]. Upper limit is 100000
     *   to ensure it never overflows to the next `REMOTE_NOTIFICATION_` value.
     * - Other notifications are always unique, and should never be grouped
     */
    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    private fun getNotificationId(
        type: NotificationType,
    ) = when (type) {
        NEWS -> NotificationIds.REMOTE_NEWS
        NEW_VERSION -> NotificationIds.REMOTE_NEW_UPDATE
        NEW_DEVICE -> NotificationIds.REMOTE_NEW_DEVICE
        GENERAL_NOTIFICATION -> NotificationIds.REMOTE_GENERAL
        else -> NotificationIds.REMOTE_UNKNOWN
    } + when (type) {
        NEWS -> messageContents[NotificationElement.NEWS_ITEM_ID.name]?.toInt() ?: 0
        NEW_DEVICE, GENERAL_NOTIFICATION -> random.nextInt(1, 100000)
        else -> 0
    }

    private fun getNotificationGroupId(
        type: NotificationType,
    ) = when (type) {
        NEW_DEVICE -> NotificationIds.REMOTE_NEW_DEVICE_GROUP
        NEWS -> NotificationIds.REMOTE_NEWS_GROUP
        GENERAL_NOTIFICATION -> NotificationIds.REMOTE_GENERAL_GROUP
        else -> NotificationIds.REMOTE_UNKNOWN_GROUP
    }

    /**
     * Used both as a group key, and the channel ID for the summary notification
     *
     * @see NotificationCompat.Builder.setGroup
     */
    private fun getNotificationGroupKey(
        type: NotificationType,
    ) = when (type) {
        NEW_VERSION -> UPDATE_NOTIFICATION_CHANNEL_ID
        NEWS -> NEWS_NOTIFICATION_CHANNEL_ID
        NEW_DEVICE -> DEVICE_NOTIFICATION_CHANNEL_ID
        GENERAL_NOTIFICATION -> GENERAL_NOTIFICATION_CHANNEL_ID
    }

    private fun getNewVersionNotificationBuilder(
        deviceName: String?,
        versionNumber: String?,
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
        message: String?,
    ) = NotificationCompat.Builder(context, NEWS_NOTIFICATION_CHANNEL_ID)
        .setPriority(PRIORITY_HIGH)
        .setContentTitle(context.getString(R.string.news_notification_channel_name))
        .setBigTextStyle(message)

    private fun getNewDeviceNotificationBuilder(
        newDeviceName: String?,
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
        message: String?,
    ) = NotificationCompat.Builder(context, GENERAL_NOTIFICATION_CHANNEL_ID)
        .setPriority(PRIORITY_DEFAULT)
        .setContentTitle(context.getString(R.string.general_notification_channel_name))
        .setBigTextStyle(message)

    private fun getNotificationIntent(
        notificationType: NotificationType,
        notificationId: Int,
    ) = PendingIntent.getActivity(
        context,
        notificationId,
        if (notificationType == NEWS) {
            Intent(context, NewsItemActivity::class.java)
                .putExtra(
                    NewsItemActivity.INTENT_NEWS_ITEM_ID,
                    messageContents[NotificationElement.NEWS_ITEM_ID.name]?.toLong()
                )
                .putExtra(NewsItemActivity.INTENT_DELAY_AD_START, true)
        } else {
            Intent(context, MainActivity::class.java)
        },
        FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else 0
    )
}
