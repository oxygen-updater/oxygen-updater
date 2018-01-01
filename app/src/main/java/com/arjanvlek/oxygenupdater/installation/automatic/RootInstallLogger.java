package com.arjanvlek.oxygenupdater.installation.automatic;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;


public class RootInstallLogger extends IntentService {

    public static final String INTENT_STATUS = "STATUS";
    public static final String INTENT_INSTALL_ID = "INSTALLATION_ID";
    public static final String INTENT_START_OS = "START_OS";
    public static final String INTENT_DESTINATION_OS = "DEST_OS";
    public static final String INTENT_CURR_OS = "CURR_OS";
    public static final String INTENT_FAILURE_REASON = "FAILURE_REASON";

    private static final String TAG = "RootInstallLogger";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public RootInstallLogger() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null || intent.getExtras() == null || !(getApplication() instanceof ApplicationData)) {
            return;
        }

        ApplicationData applicationData = (ApplicationData) getApplication();

        ServerConnector connector = applicationData.getServerConnector();
        SettingsManager settingsManager = new SettingsManager(applicationData);

        long deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L);
        long updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L);

        if (deviceId == -1L || updateMethodId == -1L) {
            Logger.logError(TAG, "Failed to log update installation action: Device and / or update method not selected.");
            return;
        }

        InstallationStatus status = (InstallationStatus) intent.getExtras().getSerializable(INTENT_STATUS);
        String installationId = intent.getExtras().getString(INTENT_INSTALL_ID, "<INVALID>");
        String startOSVersion = intent.getExtras().getString(INTENT_START_OS, "<UNKNOWN>");
        String destinationOSVersion = intent.getExtras().getString(INTENT_DESTINATION_OS, "<UNKNOWN>");
        String currentOsVersion = intent.getExtras().getString(INTENT_CURR_OS, "<UNKNOWN>");
        String timestamp = LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")).toString();
        String failureReason = intent.getExtras().getString(INTENT_FAILURE_REASON ,"");

        RootInstall installation = new RootInstall(deviceId, updateMethodId, status, installationId, timestamp, startOSVersion, destinationOSVersion, currentOsVersion, failureReason);

        connector.logRootInstall(installation, (result) -> {
            if (result == null) {
                Logger.init(applicationData);
                Logger.logError(TAG, "Failed to log update installation action: No response from server");
            } else if (!result.isSuccess()) {
                Logger.init(applicationData);
                Logger.logError(TAG, "Failed to log update installation action: " + result.getErrorMessage());
            } else if (result.isSuccess() && installation.getInstallationStatus().equals(InstallationStatus.FAILED) || installation.getInstallationStatus().equals(InstallationStatus.FINISHED)) {
                settingsManager.deletePreference(SettingsManager.PROPERTY_INSTALLATION_ID);
            }
        });


    }
}
