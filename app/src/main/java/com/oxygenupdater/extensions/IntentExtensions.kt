package com.oxygenupdater.extensions

import android.content.Intent
import android.os.Build
import androidx.core.net.toUri

fun Intent.withAppReferrer(packageName: String) = "android-app://$packageName".let {
    putExtra(Intent.EXTRA_REFERRER, it.toUri()).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            putExtra(Intent.EXTRA_REFERRER_NAME, it)
        }
    }
}
