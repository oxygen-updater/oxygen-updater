package com.oxygenupdater.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.KeyContribute
import com.oxygenupdater.services.RootFileService
import com.oxygenupdater.workers.ReadOtaDbWorker
import com.oxygenupdater.workers.WorkUniqueReadOtaDb
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import org.koin.java.KoinJavaComponent.getKoin
import java.util.concurrent.TimeUnit

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
object ContributorUtils {

    private const val TAG = "ContributorUtils"

    private val analytics by getKoin().inject<FirebaseAnalytics>()
    private val workManager by getKoin().inject<WorkManager>()

    val isAtLeastQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q // same as RootFileService
    val isAtLeastQAndPossiblyRooted
        get() = isAtLeastQ && Shell.isAppGrantedRoot() != false

    @RequiresApi(Build.VERSION_CODES.Q)
    fun flushSettings(context: Context, isContributing: Boolean) {
        val isFirstTime = !PrefManager.contains(KeyContribute)
        val wasContributing = PrefManager.getBoolean(KeyContribute, false)

        if (isFirstTime || wasContributing != isContributing) {
            PrefManager.putBoolean(KeyContribute, isContributing)

            val analyticsEventData = Bundle(2).apply {
                putString("CONTRIBUTOR_DEVICE", PrefManager.getString(PrefManager.KeyDevice, "<<UNKNOWN>>"))
                putString("CONTRIBUTOR_UPDATEMETHOD", PrefManager.getString(PrefManager.KeyUpdateMethod, "<<UNKNOWN>>"))
            }

            if (isContributing) {
                analytics.logEvent("CONTRIBUTOR_SIGNUP", analyticsEventData)
                startDbCheckingProcess(context)
            } else {
                analytics.logEvent("CONTRIBUTOR_SIGNOFF", analyticsEventData)
                stopDbCheckingProcess(context)
            }
        }
    }

    fun startDbCheckingProcess(context: Context) {
        if (!isAtLeastQ || !PrefManager.getBoolean(KeyContribute, false)) return

        startRootService(context)

        // Give time for RootFileService to copy ota.db (`setInitialDelay` doesn't work)
        Handler(Looper.getMainLooper()).postDelayed({
            workManager.enqueueUniquePeriodicWork(
                WorkUniqueReadOtaDb,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<ReadOtaDbWorker>(MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                    .build()
            )
        }, 5000) // 5s
    }

    fun stopDbCheckingProcess(context: Context) {
        if (isAtLeastQ) RootService.stop(rootServiceIntent(context))
        workManager.cancelUniqueWork(WorkUniqueReadOtaDb)
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
}
