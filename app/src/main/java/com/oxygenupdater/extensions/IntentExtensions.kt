package com.oxygenupdater.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun Intent.withAppReferrer(context: Context) = putExtra(
    Intent.EXTRA_REFERRER,
    Uri.parse("android-app://${context.packageName}")
)
