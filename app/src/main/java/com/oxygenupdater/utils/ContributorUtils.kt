package com.oxygenupdater.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest.Companion.MIN_PERIODIC_INTERVAL_MILLIS
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.set
import com.oxygenupdater.internal.settings.KeyContribute
import com.oxygenupdater.internal.settings.KeyDevice
import com.oxygenupdater.internal.settings.KeyUpdateMethod
import com.oxygenupdater.services.RootFileService
import com.oxygenupdater.workers.ReadOtaDbWorker
import com.oxygenupdater.workers.WorkUniqueReadOtaDb
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContributorUtils @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val firebaseAnalytics: FirebaseAnalytics,
) {

    fun flushSettings(context: Context, isContributing: Boolean) {
        val isFirstTime = KeyContribute !in sharedPreferences
        val wasContributing = sharedPreferences[KeyContribute, false]

        if (!isFirstTime && wasContributing == isContributing) return

        sharedPreferences[KeyContribute] = isContributing

        val analyticsEventData = Bundle(2).apply {
            putString("CONTRIBUTOR_DEVICE", sharedPreferences[KeyDevice, "<<UNKNOWN>>"])
            putString("CONTRIBUTOR_UPDATEMETHOD", sharedPreferences[KeyUpdateMethod, "<<UNKNOWN>>"])
        }

        if (isContributing) start(context).also {
            firebaseAnalytics.logEvent("CONTRIBUTOR_SIGNUP", analyticsEventData)
        } else stop(context).also {
            firebaseAnalytics.logEvent("CONTRIBUTOR_SIGNOFF", analyticsEventData)
        }
    }

    fun startOrStop(context: Context) = hasRootAccess {
        if (it) start(context) else stop(context)
    }

    private fun start(context: Context) {
        if (!isAtLeastQ || !sharedPreferences[KeyContribute, false]) return

        startRootService(context)

        // Give time for RootFileService to copy ota.db (`setInitialDelay` doesn't work)
        Handler(Looper.getMainLooper()).postDelayed({
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WorkUniqueReadOtaDb,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<ReadOtaDbWorker>(MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                    .build()
            )
        }, 5000) // 5s
    }

    private fun stop(context: Context) {
        if (isAtLeastQ) RootService.stop(rootServiceIntent(context))
        WorkManager.getInstance(context).cancelUniqueWork(WorkUniqueReadOtaDb)
    }

    /** Does nothing if app doesn't have root access */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun startRootService(context: Context) {
        if (RootFileService.bound.get()) return // already bound
        RootService.bind(rootServiceIntent(context), EmptyServiceConnection)
    }

    /**
     * Launch in daemon mode to run in the background independent of app lifecycle
     *
     * Note: this doesn't prevent service from terminating if app was updated or deleted
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun rootServiceIntent(context: Context) = Intent(context, RootFileService::class.java)
        .addCategory(RootService.CATEGORY_DAEMON_MODE)

    private object EmptyServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder) = logVerbose(TAG, "onServiceConnected")
        override fun onServiceDisconnected(name: ComponentName?) = logVerbose(TAG, "onServiceDisconnected")
        override fun onBindingDied(name: ComponentName?) = logVerbose(TAG, "onBindingDied")
        override fun onNullBinding(name: ComponentName?) = logVerbose(TAG, "onNullBinding")
    }

    companion object {
        private const val TAG = "ContributorUtils"

        val isAtLeastQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q // same as RootFileService
        val isAtLeastQAndPossiblyRooted
            get() = isAtLeastQ && Shell.isAppGrantedRoot() != false
    }
}
