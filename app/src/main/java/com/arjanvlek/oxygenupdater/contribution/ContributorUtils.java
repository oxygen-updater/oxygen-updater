package com.arjanvlek.oxygenupdater.contribution;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Objects;

import static com.arjanvlek.oxygenupdater.views.MainActivity.PERMISSION_REQUEST_CODE;
import static com.arjanvlek.oxygenupdater.views.MainActivity.VERIFY_FILE_PERMISSION;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 02/05/2019.
 */
public class ContributorUtils {

    private static final int CONTRIBUTOR_FILE_SCANNER_JOB_ID = 424242;
    private static final int CONTRIBUTOR_FILE_SCANNER_INTERVAL_MINUTES = 15;

    private final Context context;

    public ContributorUtils(Context context) {
        Objects.requireNonNull(context, "Context cannot be null");
        this.context = context;
    }

    public void flushSettings(boolean isContributing) {
        SettingsManager settingsManager = new SettingsManager(context);

        boolean wasContributing = settingsManager.getPreference(SettingsManager.PROPERTY_CONTRIBUTE, false);

        if (wasContributing != isContributing) {
            settingsManager.savePreference(SettingsManager.PROPERTY_CONTRIBUTE, isContributing);

            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            Bundle analyticsEventData = new Bundle();

            analyticsEventData.putString("CONTRIBUTOR_DEVICE", settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE, "<<UNKNOWN>>"));
            analyticsEventData.putString("CONTRIBUTOR_UPDATEMETHOD", settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "<<UNKNOWN>>"));

            if (isContributing) {
                analytics.logEvent("CONTRIBUTOR_SIGNUP", analyticsEventData);
                startFileCheckingProcess();
            } else {
                analytics.logEvent("CONTRIBUTOR_SIGNOFF", analyticsEventData);
                stopFileCheckingProcess();
            }
        }
    }

    private void startFileCheckingProcess() {
        JobInfo.Builder task = new JobInfo.Builder(CONTRIBUTOR_FILE_SCANNER_JOB_ID, new ComponentName(context, UpdateFileChecker.class))
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setPersisted(true)
                .setPeriodic(CONTRIBUTOR_FILE_SCANNER_INTERVAL_MINUTES * 60 * 1000);

        if (Build.VERSION.SDK_INT >= 26) {
            task.setRequiresBatteryNotLow(false);
            task.setRequiresStorageNotLow(false);
        }

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(task.build());

        if (resultCode != JobScheduler.RESULT_SUCCESS) {
            Logger.logWarning("ContributorActivity", new ContributorException("File check could not be scheduled. Exit code of scheduler: " + resultCode));
        }
    }

    private void stopFileCheckingProcess() {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(CONTRIBUTOR_FILE_SCANNER_JOB_ID);
    }
}
