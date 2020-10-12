package com.oxygenupdater.utils

import androidx.core.os.bundleOf
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.internal.settings.SettingsManager.PROPERTY_CONTRIBUTE
import com.oxygenupdater.workers.CheckSystemUpdateFilesWorker
import com.oxygenupdater.workers.WORK_UNIQUE_CHECK_SYSTEM_UPDATE_FILES
import org.koin.java.KoinJavaComponent.inject
import java.util.concurrent.TimeUnit

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
object ContributorUtils {

    private val analytics by inject(FirebaseAnalytics::class.java)
    private val workManager by inject(WorkManager::class.java)

    fun flushSettings(isContributing: Boolean) {
        val isFirstTime = !SettingsManager.containsPreference(PROPERTY_CONTRIBUTE)
        val wasContributing = SettingsManager.getPreference(PROPERTY_CONTRIBUTE, false)

        if (isFirstTime || wasContributing != isContributing) {
            SettingsManager.savePreference(PROPERTY_CONTRIBUTE, isContributing)

            val analyticsEventData = bundleOf(
                "CONTRIBUTOR_DEVICE" to SettingsManager.getPreference(SettingsManager.PROPERTY_DEVICE, "<<UNKNOWN>>"),
                "CONTRIBUTOR_UPDATEMETHOD" to SettingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "<<UNKNOWN>>")
            )

            if (isContributing) {
                analytics.logEvent("CONTRIBUTOR_SIGNUP", analyticsEventData)
                startFileCheckingProcess()
            } else {
                analytics.logEvent("CONTRIBUTOR_SIGNOFF", analyticsEventData)
                stopFileCheckingProcess()
            }
        }
    }

    private fun startFileCheckingProcess() {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<CheckSystemUpdateFilesWorker>(
            MIN_PERIODIC_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS
        ).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_UNIQUE_CHECK_SYSTEM_UPDATE_FILES,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
    }

    private fun stopFileCheckingProcess() = workManager.cancelUniqueWork(
        WORK_UNIQUE_CHECK_SYSTEM_UPDATE_FILES
    )
}
