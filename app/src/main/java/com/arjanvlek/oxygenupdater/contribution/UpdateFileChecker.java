package com.arjanvlek.oxygenupdater.contribution;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Environment;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.internal.server.ServerPostResult;
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.download.DownloadService.DIRECTORY_ROOT;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 02/05/2019.
 */
@SuppressWarnings("Convert2Lambda")
public class UpdateFileChecker extends JobService {

    private static final String[] UPDATE_DIRECTORIES = new String[] {".Ota"};
    private static final String TAG = "UpdateFileChecker";
    private SubmittedUpdateFileRepository repository;

    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            return performFileCheck(params);
        } catch (Exception e) {
            Logger.logError(TAG, "Error in scheduled update file name check", e);
            jobFinished(params, false);
            return true; // Do not show any failures / crashes to the user - this is a background process.
        }
    }

    private boolean performFileCheck(JobParameters params) {
        Logger.logDebug(TAG, "Started update file check");


        if (!Utils.checkNetworkConnection(getApplicationContext())) {
            Logger.logDebug(TAG, "Network unavailable, skipping update file check");
            jobFinished(params, false);
            return true; // do not perform any action without network.
        }

        AtomicInteger openNetworkCalls = new AtomicInteger(0);
        repository = new SubmittedUpdateFileRepository(getApplicationContext());

        for (String directoryName : UPDATE_DIRECTORIES) {
            File directory = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT), directoryName);

            if (!directory.exists()) {
                Logger.logDebug(TAG, "Directory: " + directory.getAbsolutePath() + " does not exist. Skipping...");
                continue;
            }

            Logger.logDebug(TAG, "Started checking for update files in directory: " + directory.getAbsolutePath());

            List<String> fileNamesInDirectory = new ArrayList<>();
            getAllFileNames(directory, fileNamesInDirectory);

            for (String fileName : fileNamesInDirectory) {
                Logger.logDebug(TAG, "Found update file: " + fileName);
                if (!repository.isFileAlreadySubmitted(fileName)) {

                    Consumer<ServerPostResult> callback = new Consumer<ServerPostResult>() {
                        @Override
                        public void accept(ServerPostResult serverPostResult) {
                            if (serverPostResult == null) {
                                // network error, try again later
                                Logger.logWarning(TAG, "Error submitting update file " + fileName + ": No network connection or empty response");
                            } else if (!serverPostResult.isSuccess()) {
                                // server error, try again later
                                Logger.logError(TAG, "Error submitting update file " + fileName + ": " + serverPostResult.getErrorMessage());
                            } else {
                                Logger.logInfo(TAG, "Successfully submitted update file " + fileName);
                                // Inform user of successful contribution (only if the file is not a "bogus" temporary file)
                                if (fileName != null && fileName.contains(".zip")) {
                                    LocalNotifications.showContributionSuccessfulNotification(getApplication(), fileName.replace(".tmp", ""));
                                    // Increase number of submitted updates. Not currently shown in the UI, but may come in handy later.
                                    SettingsManager settingsManager = new SettingsManager(getApplication());
                                    settingsManager.savePreference(SettingsManager.PROPERTY_CONTRIBUTION_COUNT, settingsManager.getPreference(SettingsManager.PROPERTY_CONTRIBUTION_COUNT, 0) + 1);
                                    // Store the filename in a local database to prevent re-submission until it gets installed by the user.
                                }

                                repository.store(fileName);
                            }

                            if (openNetworkCalls.decrementAndGet() <= 0) {
                                jobFinished(params, false);
                            }
                        }
                    };
                    Logger.logDebug(TAG, "Submitting update file " + fileName);
                    openNetworkCalls.incrementAndGet();
                    ServerConnector serverConnector = ((ApplicationData)getApplication()).getServerConnector();
                    serverConnector.submitUpdateFile(fileName, callback);
                } else {
                    Logger.logDebug(TAG, "Update file " + fileName + " has already been submitted. Ignoring...");
                }
            }

            Logger.logDebug(TAG, "Finished checking for update files in directory: " + directory.getAbsolutePath());
        }

        if (openNetworkCalls.get() <= 0) {
            jobFinished(params, false);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (repository != null) {
            repository.close();
            repository = null;
        }
        return true;
    }



    private void getAllFileNames(final File folder, List<String> result) {
        for (final File f : folder.listFiles()) {

            if (f.isDirectory()) {
                getAllFileNames(f, result);
            }

            if (f.isFile()) {
                result.add(f.getName());
            }
        }
    }
}
