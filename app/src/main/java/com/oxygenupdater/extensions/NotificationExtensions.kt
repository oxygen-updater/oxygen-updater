package com.oxygenupdater.extensions

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

fun NotificationCompat.Builder.setBigTextStyle(text: String?) = setContentText(text).setStyle(
    NotificationCompat.BigTextStyle().bigText(text)
)

@Suppress("NOTHING_TO_INLINE")
inline fun NotificationManagerCompat.tryNotify(
    id: Int,
    notification: Notification,
) = try {
    notify(id, notification)
} catch (e: SecurityException) {
    // ignore; user didn't grant notification permission
}
