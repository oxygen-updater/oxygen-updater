package com.oxygenupdater.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun Intent.withAppReferrer(context: Context) = "android-app://${context.packageName}".let {
    putExtra(
        Intent.EXTRA_REFERRER,
        Uri.parse(it)
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            putExtra(Intent.EXTRA_REFERRER_NAME, it)
        }
    }
}
