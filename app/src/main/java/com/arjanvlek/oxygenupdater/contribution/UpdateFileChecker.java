package com.arjanvlek.oxygenupdater.contribution;

import android.app.Application;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Bundle;
import android.os.Environment;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.server.NetworkException;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.internal.server.ServerPostResult;
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.download.DownloadService.DIRECTORY_ROOT;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logInfo;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 02/05/2019.
 */
@SuppressWarnings("Convert2Lambda")
public class UpdateFileChecker extends JobService {

	private static final String[] UPDATE_DIRECTORIES = new String[]{".Ota"};
	private static final String TAG = "UpdateFileChecker";
	private static final String FILE_ALREADY_IN_DATABASE = "E_FILE_ALREADY_IN_DB";
	private static final String FILENAME_INVALID = "E_FILE_INVALID";
	private SubmittedUpdateFileRepository repository;

	@Override
	public boolean onStartJob(JobParameters params) {
		try {
			return performFileCheck(params);
		} catch (Exception e) {
			logError(TAG, "Error in scheduled update file name check", e);
			jobFinished(params, false);
			return true; // Do not show any failures / crashes to the user - this is a background process.
		}
	}

	@Override
	public boolean onStopJob(JobParameters params) {
		if (repository != null) {
			repository.close();
			repository = null;
		}
		return true;
	}

	private boolean performFileCheck(JobParameters params) {
		logDebug(TAG, "Started update file check");

		if (!Utils.checkNetworkConnection(getApplicationContext())) {
			logDebug(TAG, "Network unavailable, skipping update file check");
			jobFinished(params, false);
			return true; // do not perform any action without network.
		}

		AtomicInteger openNetworkCalls = new AtomicInteger(0);
		repository = new SubmittedUpdateFileRepository(getApplicationContext());

		for (String directoryName : UPDATE_DIRECTORIES) {
			File directory = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT), directoryName);

			if (!directory.exists()) {
				logDebug(TAG, "Directory: " + directory.getAbsolutePath() + " does not exist. Skipping...");
				continue;
			}

			logDebug(TAG, "Started checking for update files in directory: " + directory.getAbsolutePath());

			List<String> fileNamesInDirectory = new ArrayList<>();
			getAllFileNames(directory, fileNamesInDirectory);

			for (String fileName : fileNamesInDirectory) {
				logDebug(TAG, "Found update file: " + fileName);
				if (!repository.isFileAlreadySubmitted(fileName)) {

					Application application = getApplication();
					Consumer<ServerPostResult> callback = new Consumer<ServerPostResult>() {
						@Override
						public void accept(ServerPostResult serverPostResult) {
							if (serverPostResult == null) {
								// network error, try again later
								logWarning(TAG, new NetworkException("Error submitting update file " + fileName + ": No network connection or empty response"));
							} else if (!serverPostResult.isSuccess()) {
								String errorMessage = serverPostResult.getErrorMessage();
								// If file is already in our database or if file is an invalid temporary file (server decides when this is the case),
								// mark this file as submitted but don't inform the user about it.
								if (errorMessage != null
										&& (errorMessage.equals(FILE_ALREADY_IN_DATABASE) || errorMessage.equals(FILENAME_INVALID))) {
									logInfo(TAG, "Ignoring submitted update file " + fileName + ", already in database or not relevant");
									repository.store(fileName);

									// Log failed contribution
									Bundle analyticsBundle = new Bundle();
									analyticsBundle.putString("CONTRIBUTION_FILENAME", fileName);
									FirebaseAnalytics.getInstance(application).logEvent("CONTRIBUTION_NOT_NEEDED", analyticsBundle);
								} else {
									// server error, try again later
									logError(TAG, new NetworkException("Error submitting update file " + fileName + ": " + serverPostResult.getErrorMessage()));
								}
							} else {
								logInfo(TAG, "Successfully submitted update file " + fileName);
								// Inform user of successful contribution (only if the file is not a "bogus" temporary file)
								if (fileName != null && fileName.contains(".zip")) {
									LocalNotifications.showContributionSuccessfulNotification(application, fileName);
									// Increase number of submitted updates. Not currently shown in the UI, but may come in handy later.
									SettingsManager settingsManager = new SettingsManager(application);
									settingsManager.savePreference(
											SettingsManager.PROPERTY_CONTRIBUTION_COUNT,
											settingsManager.getPreference(SettingsManager.PROPERTY_CONTRIBUTION_COUNT, 0) + 1
									);

									// Log successful contribution
									Bundle analyticsBundle = new Bundle();
									analyticsBundle.putString("CONTRIBUTION_FILENAME", fileName);
									FirebaseAnalytics.getInstance(application).logEvent("CONTRIBUTION_SUCCESSFUL", analyticsBundle);
								}

								// Store the filename in a local database to prevent re-submission until it gets installed or removed by the user.
								repository.store(fileName);
							}

							if (openNetworkCalls.decrementAndGet() <= 0) {
								jobFinished(params, false);
							}
						}
					};
					logDebug(TAG, "Submitting update file " + fileName);
					openNetworkCalls.incrementAndGet();
					ServerConnector serverConnector = ((ApplicationData) application).getServerConnector();
					serverConnector.submitUpdateFile(fileName, callback);
				} else {
					logDebug(TAG, "Update file " + fileName + " has already been submitted. Ignoring...");
				}
			}

			logDebug(TAG, "Finished checking for update files in directory: " + directory.getAbsolutePath());
		}

		if (openNetworkCalls.get() <= 0) {
			jobFinished(params, false);
		}
		return true;
	}

	private void getAllFileNames(File folder, List<String> result) {
		File[] files = folder.listFiles();

		// Happens when the user has revoked the "read external storage" permission of the app
		if (files == null) {
			return;
		}

		for (File f : files) {

			if (f.isDirectory()) {
				getAllFileNames(f, result);
			}

			if (f.isFile()) {
				result.add(f.getName());
			}
		}
	}
}
