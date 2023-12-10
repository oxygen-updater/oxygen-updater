package com.oxygenupdater.extensions

import androidx.core.app.NotificationCompat

fun NotificationCompat.Builder.setBigTextStyle(text: String?) = setContentText(text).setStyle(
    NotificationCompat.BigTextStyle().bigText(text)
)
