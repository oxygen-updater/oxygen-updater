package com.oxygenupdater.extensions

import android.content.Intent
import androidx.core.net.toUri

fun Intent.withAppReferrer(packageName: String) = "android-app://$packageName".let {
    putExtra(Intent.EXTRA_REFERRER, it.toUri())
    putExtra(Intent.EXTRA_REFERRER_NAME, it)
}
