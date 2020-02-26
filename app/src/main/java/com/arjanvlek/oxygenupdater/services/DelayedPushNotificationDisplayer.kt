package com.arjanvlek.oxygenupdater.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.activities.MainActivity
import com.arjanvlek.oxygenupdater.activities.NewsActivity
import com.arjanvlek.oxygenupdater.enums.NotificationElement
import com.arjanvlek.oxygenupdater.enums.NotificationElement.DEVICE_NAME
import com.arjanvlek.oxygenupdater.enums.NotificationElement.DUTCH_MESSAGE
import com.arjanvlek.oxygenupdater.enums.NotificationElement.ENGLISH_MESSAGE
import com.arjanvlek.oxygenupdater.enums.NotificationElement.NEW_DEVICE_NAME
import com.arjanvlek.oxygenupdater.enums.NotificationElement.NEW_VERSION_NUMBER
import com.arjanvlek.oxygenupdater.enums.NotificationElement.TYPE
import com.arjanvlek.oxygenupdater.enums.NotificationType
import com.arjanvlek.oxygenupdater.enums.NotificationType.GENERAL_NOTIFICATION
import com.arjanvlek.oxygenupdater.enums.NotificationType.NEWS
import com.arjanvlek.oxygenupdater.enums.NotificationType.NEW_DEVICE
import com.arjanvlek.oxygenupdater.enums.NotificationType.NEW_VERSION
import com.arjanvlek.oxygenupdater.exceptions.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.objectMapper
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.AppLocale
import com.arjanvlek.oxygenupdater.models.AppLocale.NL
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.Utils
import com.fasterxml.jackson.core.type.TypeReference
import org.koin.android.ext.android.inject
import java.io.IOException

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
 */
class DelayedPushNotificationDisplayer : JobService() {

    private val settingsManager by inject<SettingsManager>()

    override fun onStartJob(params: JobParameters?): Boolean {
        if (params == null || !params.extras.containsKey(KEY_NOTIFICATION_CONTENTS) || application !is OxygenUpdater) {
            return exit(params, false)
        }

        // Get notification contents of FCM.
        val notificationContentsTypeRef: TypeReference<Map<String, String>> = object : TypeReference<Map<String, String>>() {}
        val notificationContentsJson = params.extras.getString(KEY_NOTIFICATION_CONTENTS)

        val messageContents = try {
            objectMapper.readValue(notificationContentsJson, notificationContentsTypeRef)
        } catch (e: IOException) {
            logError(
                TAG,
                OxygenUpdaterException("Failed to read notification contents from JSON string ($notificationContentsJson)")
            )
            return exit(params, false)
        }

        val notificationType = NotificationType.valueOf(messageContents[TYPE.toString()] ?: "")

        val builder: NotificationCompat.Builder? = when (notificationType) {
            NEW_DEVICE -> if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS, true)) {
                return exit(params, true)
            } else {
                getBuilderForNewDeviceNotification(messageContents[NEW_DEVICE_NAME.toString()])
            }
            NEW_VERSION -> if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS, true)) {
                return exit(params, true)
            } else {
                getBuilderForNewVersionNotification(messageContents[DEVICE_NAME.toString()], messageContents[NEW_VERSION_NUMBER.toString()])
            }
            GENERAL_NOTIFICATION -> if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS, true)) {
                return exit(params, true)
            } else {
                val message = if (AppLocale.get() == NL) messageContents[DUTCH_MESSAGE.toString()] else messageContents[ENGLISH_MESSAGE.toString()]

                getBuilderForGeneralServerNotificationOrNewsNotification(message)
            }
            NEWS -> if (!settingsManager.getPreference(SettingsManager.PROPERTY_RECEIVE_NEWS_NOTIFICATIONS, true)) {
                return exit(params, true)
            } else {
                val newsMessage = if (AppLocale.get() == NL) messageContents[DUTCH_MESSAGE.toString()] else messageContents[ENGLISH_MESSAGE.toString()]

                getBuilderForGeneralServerNotificationOrNewsNotification(newsMessage)
            }
        }

        if (builder == null) {
            logError(
                TAG,
                OxygenUpdaterException("Failed to instantiate notificationBuilder. Can not display push notification!")
            )
            return exit(params, false)
        }

        builder.setContentIntent(getNotificationIntent(notificationType, messageContents))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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

    override fun onStopJob(params: JobParameters) = true

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    private fun getNotificationId(type: NotificationType) = when (type) {
        NEW_DEVICE -> NEW_DEVICE_NOTIFICATION_ID
        NEW_VERSION -> NEW_UPDATE_NOTIFICATION_ID
        GENERAL_NOTIFICATION -> GENERIC_NOTIFICATION_ID
        NEWS -> NEWS_NOTIFICATION_ID
        else -> UNKNOWN_NOTIFICATION_ID
    }

    private fun getBuilderForGeneralServerNotificationOrNewsNotification(message: String?) = notificationBuilder
        .setSmallIcon(R.drawable.logo_outline)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setContentTitle(getString(R.string.app_name))
        .setContentText(message)

    private fun getBuilderForNewDeviceNotification(newDeviceName: String?) = getString(
        R.string.notification_new_device,
        newDeviceName
    ).let {
        notificationBuilder
            .setSmallIcon(R.drawable.new_text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(it).setSummaryText(getString(R.string.notification_new_device_short)))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(it)
    }

    private fun getBuilderForNewVersionNotification(deviceName: String?, versionNumber: String?) = getString(
        R.string.notification_version,
        versionNumber,
        deviceName
    ).let {
        notificationBuilder
            .setSmallIcon(R.drawable.new_text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(it))
            .setWhen(System.currentTimeMillis())
            .setContentTitle(getString(R.string.notification_version_title))
            .setContentText(it)
    }


    private val notificationBuilder = if (Build.VERSION.SDK_INT >= 26) {
        NotificationCompat.Builder(this, OxygenUpdater.PUSH_NOTIFICATION_CHANNEL_ID)
    } else {
        @Suppress("DEPRECATION")
        NotificationCompat.Builder(this)
    }

    private fun getNotificationIntent(notificationType: NotificationType, messageContents: Map<String, String>) = if (notificationType == NEWS) {
        val newsIntent = Intent(this, NewsActivity::class.java)
            .putExtra(NewsActivity.INTENT_NEWS_ITEM_ID, messageContents[NotificationElement.NEWS_ITEM_ID.toString()]?.toLong())
            .putExtra(NewsActivity.INTENT_START_WITH_AD, true)

        PendingIntent.getActivity(this, 0, newsIntent, 0)
    } else {
        PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)
    }

    private fun exit(parameters: JobParameters?, success: Boolean) = jobFinished(parameters, !success).let {
        success
    }

    companion object {
        private const val TAG = "DelayedPushNotificationDisplayer"

        const val NEW_DEVICE_NOTIFICATION_ID = 10010
        const val NEW_UPDATE_NOTIFICATION_ID = 20020
        const val GENERIC_NOTIFICATION_ID = 30030
        const val NEWS_NOTIFICATION_ID = 50050
        const val UNKNOWN_NOTIFICATION_ID = 40040
        const val KEY_NOTIFICATION_CONTENTS = "notification-contents"
    }
}
