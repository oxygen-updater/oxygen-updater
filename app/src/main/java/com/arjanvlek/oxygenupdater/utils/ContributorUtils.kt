package com.arjanvlek.oxygenupdater.utils

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.core.os.bundleOf
import com.arjanvlek.oxygenupdater.exceptions.ContributorException
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager.Companion.PROPERTY_CONTRIBUTE
import com.arjanvlek.oxygenupdater.services.UpdateFileChecker
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.java.KoinJavaComponent.inject

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
 */
class ContributorUtils(private val context: Context) {

    private val settingsManager by inject(SettingsManager::class.java)

    fun flushSettings(isContributing: Boolean) {
        val isFirstTime = !settingsManager.containsPreference(PROPERTY_CONTRIBUTE)
        val wasContributing = settingsManager.getPreference(PROPERTY_CONTRIBUTE, false)

        if (isFirstTime || wasContributing != isContributing) {
            settingsManager.savePreference(PROPERTY_CONTRIBUTE, isContributing)

            val analytics = FirebaseAnalytics.getInstance(context)

            val analyticsEventData = bundleOf(
                "CONTRIBUTOR_DEVICE" to settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE, "<<UNKNOWN>>"),
                "CONTRIBUTOR_UPDATEMETHOD" to settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "<<UNKNOWN>>")
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
        val task = JobInfo.Builder(CONTRIBUTOR_FILE_SCANNER_JOB_ID, ComponentName(context, UpdateFileChecker::class.java))
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .setPersisted(true)
            .setPeriodic(CONTRIBUTOR_FILE_SCANNER_INTERVAL_MINUTES * 60 * 1000.toLong())

        if (Build.VERSION.SDK_INT >= 26) {
            task.setRequiresBatteryNotLow(false)
            task.setRequiresStorageNotLow(false)
        }

        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val resultCode = scheduler.schedule(task.build())

        if (resultCode != JobScheduler.RESULT_SUCCESS) {
            logWarning(
                "ContributorActivity",
                ContributorException("File check could not be scheduled. Exit code of scheduler: $resultCode")
            )
        }
    }

    private fun stopFileCheckingProcess() = (context.getSystemService(
        Context.JOB_SCHEDULER_SERVICE
    ) as JobScheduler).cancel(CONTRIBUTOR_FILE_SCANNER_JOB_ID)

    companion object {
        private const val CONTRIBUTOR_FILE_SCANNER_JOB_ID = 424242
        private const val CONTRIBUTOR_FILE_SCANNER_INTERVAL_MINUTES = 15
    }
}
