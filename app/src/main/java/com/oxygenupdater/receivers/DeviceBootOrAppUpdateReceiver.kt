package com.oxygenupdater.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.oxygenupdater.extensions.get
import com.oxygenupdater.internal.settings.KeyContribute
import com.oxygenupdater.utils.ContributorUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * OnePlus devices have very aggressive battery restrictions. By default, apps don't receive
 * [Intent.ACTION_BOOT_COMPLETED] even if [android.Manifest.permission.RECEIVE_BOOT_COMPLETED]
 * is specified. OOS12+ has a per-app toggle for "Allow auto-launch", which must also be enabled
 * for this receiver to work.
 */
@AndroidEntryPoint
class DeviceBootOrAppUpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var contributorUtils: ContributorUtils

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!ContributorUtils.isAtLeastQ || !sharedPreferences[KeyContribute, false]) return

        Handler(Looper.getMainLooper()).postDelayed({
            contributorUtils.startOrStop(context)
        }, 5000) // 5s
    }
}
