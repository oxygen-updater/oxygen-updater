package com.arjanvlek.oxygenupdater.contribution

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.Objects.requireNonNull

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 02/05/2019.
 */
class ContributorUtils(private val context: Context) {

    init {
        requireNonNull(context, "Context cannot be null")
    }

    fun flushSettings(isContributing: Boolean) {
        val settingsManager = SettingsManager(context)

        val isFirstTime = !settingsManager.containsPreference(SettingsManager.PROPERTY_CONTRIBUTE)
        val wasContributing = settingsManager.getPreference(SettingsManager.PROPERTY_CONTRIBUTE, false)

        if (isFirstTime || wasContributing != isContributing) {
            settingsManager.savePreference(SettingsManager.PROPERTY_CONTRIBUTE, isContributing)

            val analytics = FirebaseAnalytics.getInstance(context)
            val analyticsEventData = Bundle()

            analyticsEventData.putString("CONTRIBUTOR_DEVICE", settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE, "<<UNKNOWN>>"))
            analyticsEventData.putString("CONTRIBUTOR_UPDATEMETHOD", settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "<<UNKNOWN>>"))

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
                .setPeriodic((CONTRIBUTOR_FILE_SCANNER_INTERVAL_MINUTES * 60 * 1000).toLong())

        if (Build.VERSION.SDK_INT >= 26) {
            task.setRequiresBatteryNotLow(false)
            task.setRequiresStorageNotLow(false)
        }

        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val resultCode = scheduler.schedule(task.build())

        if (resultCode != JobScheduler.RESULT_SUCCESS) {
            logWarning("ContributorActivity", ContributorException("File check could not be scheduled. Exit code of scheduler: $resultCode"))
        }
    }

    private fun stopFileCheckingProcess() {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancel(CONTRIBUTOR_FILE_SCANNER_JOB_ID)
    }

    companion object {
        private const val CONTRIBUTOR_FILE_SCANNER_JOB_ID = 424242
        private const val CONTRIBUTOR_FILE_SCANNER_INTERVAL_MINUTES = 15
    }
}
