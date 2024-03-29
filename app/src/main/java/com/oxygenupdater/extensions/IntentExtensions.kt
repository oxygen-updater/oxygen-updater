package com.oxygenupdater.extensions

import android.content.Intent
import android.net.Uri
import android.os.Build

fun Intent.withAppReferrer(packageName: String) = "android-app://$packageName".let {
    putExtra(Intent.EXTRA_REFERRER, Uri.parse(it)).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            putExtra(Intent.EXTRA_REFERRER_NAME, it)
        }
    }
}
