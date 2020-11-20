package com.oxygenupdater.extensions

import androidx.core.app.NotificationCompat

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun NotificationCompat.Builder.setBigTextStyle(
    text: String?
): NotificationCompat.Builder = setContentText(text).setStyle(
    NotificationCompat.BigTextStyle().bigText(text)
)
