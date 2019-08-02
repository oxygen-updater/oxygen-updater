package com.arjanvlek.oxygenupdater.installation.automatic;

import android.app.job.JobParameters;
import android.app.job.JobService;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.internal.server.NetworkException;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;


public class RootInstallLogger extends JobService {

	public static final String DATA_STATUS = "STATUS";
	public static final String DATA_INSTALL_ID = "INSTALLATION_ID";
	public static final String DATA_START_OS = "START_OS";
	public static final String DATA_DESTINATION_OS = "DEST_OS";
	public static final String DATA_CURR_OS = "CURR_OS";
	public static final String DATA_FAILURE_REASON = "FAILURE_REASON";

	private static final String TAG = "RootInstallLogger";

	@Override
	public boolean onStartJob(JobParameters params) {
		if (params == null || !(getApplication() instanceof ApplicationData)) {
			return true; // Retrying wont fix this issue. This is a lost cause.
		}

		ApplicationData applicationData = (ApplicationData) getApplication();

		ServerConnector connector = applicationData.getServerConnector();
		SettingsManager settingsManager = new SettingsManager(applicationData);

		long deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L);
		long updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L);

		if (deviceId == -1L || updateMethodId == -1L) {
			Logger.logError(TAG, new OxygenUpdaterException("Failed to log update installation action: Device and / or update method not selected."));
			return true; // Retrying wont fix this issue. This is a lost cause.
		}

		InstallationStatus status = InstallationStatus.valueOf(params.getExtras()
				.getString(DATA_STATUS));
		String installationId = params.getExtras().getString(DATA_INSTALL_ID, "<INVALID>");
		String startOSVersion = params.getExtras().getString(DATA_START_OS, "<UNKNOWN>");
		String destinationOSVersion = params.getExtras()
				.getString(DATA_DESTINATION_OS, "<UNKNOWN>");
		String currentOsVersion = params.getExtras().getString(DATA_CURR_OS, "<UNKNOWN>");
		String timestamp = LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")).toString();
		String failureReason = params.getExtras().getString(DATA_FAILURE_REASON, "");

		RootInstall installation = new RootInstall(deviceId, updateMethodId, status, installationId, timestamp, startOSVersion, destinationOSVersion, currentOsVersion, failureReason);

		connector.logRootInstall(installation, (result) -> {
			if (result == null) {
				Logger.logError(TAG, new NetworkException("Failed to log update installation action on server: No response from server"));
				jobFinished(params, true);
			} else if (!result.isSuccess()) {
				Logger.logError(TAG, new OxygenUpdaterException("Failed to log update installation action on server: " + result
						.getErrorMessage()));
				jobFinished(params, true);
			} else if (result.isSuccess() && installation.getInstallationStatus()
					.equals(InstallationStatus.FAILED) || installation.getInstallationStatus()
					.equals(InstallationStatus.FINISHED)) {
				settingsManager.deletePreference(SettingsManager.PROPERTY_INSTALLATION_ID);
				jobFinished(params, false);
			} else {
				jobFinished(params, false);
			}
		});

		return true;
	}

	@Override
	public boolean onStopJob(JobParameters params) {
		return true;
	}
}
