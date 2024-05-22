package com.oxygenupdater.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.oxygenupdater.extensions.get
import com.oxygenupdater.internal.settings.KeyContribute
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.ContributorUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

    @Inject
    lateinit var serverRepository: ServerRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        @Suppress("DeferredResultUnused")
        CoroutineScope(Dispatchers.IO).async {
            // Ping server to track OTA versions (not PII)
            serverRepository.osInfoHeartbeat(action.substringAfterLast('.'))
        }

        if (!ContributorUtils.isAtLeastQ || !sharedPreferences[KeyContribute, false]) return

        Handler(Looper.getMainLooper()).postDelayed({
            contributorUtils.startOrStop(context)
        }, 5000) // 5s
    }
}
