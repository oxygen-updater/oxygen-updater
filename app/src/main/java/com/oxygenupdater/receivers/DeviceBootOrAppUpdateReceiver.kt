package com.oxygenupdater.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.hasRootAccess

/**
 * OnePlus devices have very aggressive battery restrictions. By default, apps don't receive
 * [Intent.ACTION_BOOT_COMPLETED] even if [android.Manifest.permission.RECEIVE_BOOT_COMPLETED]
 * is specified. OOS12+ has a per-app toggle for "Allow auto-launch", which must also be enabled
 * for this receiver to work.
 */
class DeviceBootOrAppUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!ContributorUtils.isAtLeastQ || !PrefManager.getBoolean(PrefManager.PROPERTY_CONTRIBUTE, false)) return

        Handler(Looper.getMainLooper()).postDelayed({
            hasRootAccess {
                if (!it) return@hasRootAccess

                ContributorUtils.startRootService(context)
            }
        }, 5000) // 5s
    }
}
