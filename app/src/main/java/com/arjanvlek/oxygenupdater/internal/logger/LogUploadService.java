package com.arjanvlek.oxygenupdater.internal.logger;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import org.json.JSONException;
import org.json.JSONObject;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

public class LogUploadService extends JobService {

    private static final String TAG = "LogUploadService";

    public static final String DATA_EVENT_TYPE = "event_type";
    public static final String DATA_TAG = "tag";
    public static final String DATA_MESSAGE = "message";
    public static final String DATA_EVENT_DATE = "event_date";

    @Override
    public boolean onStartJob(JobParameters parameters) {
        if (parameters == null || !(getApplication() instanceof ApplicationData)) {
            return true; // Exit because the job cannot start.
        }

        String logLevel = parameters.getExtras().getString(DATA_EVENT_TYPE);
        String tag = parameters.getExtras().getString(DATA_TAG);
        String message = parameters.getExtras().getString(DATA_MESSAGE);
        String eventDate = parameters.getExtras().getString(DATA_EVENT_DATE);

        SettingsManager settingsManager = new SettingsManager(getApplication());
        ServerConnector serverConnector = new ServerConnector(settingsManager, false);
        SystemVersionProperties systemVersionProperties = new SystemVersionProperties(false);

        serverConnector.getDevices(false, devices -> {
            boolean deviceIsSupported = Utils.isSupportedDevice(systemVersionProperties, devices);

            try {
                JSONObject logData = new JSONObject();
                logData.put("event_type", logLevel);
                logData.put("device_is_supported", deviceIsSupported);
                logData.put("device_id", settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L));
                logData.put("update_method_id", settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L));
                logData.put("device_name", Build.BRAND + " " + Build.PRODUCT + " (" + Build.BOARD + ")");
                logData.put("operating_system_version", !systemVersionProperties.getOxygenOSOTAVersion().equals(NO_OXYGEN_OS) ? systemVersionProperties.getOxygenOSOTAVersion() : "Android " + Build.VERSION.RELEASE);
                logData.put("error_message", tag + " : " + message);
                logData.put("app_version", BuildConfig.VERSION_NAME);
                logData.put("event_date", eventDate);


                serverConnector.log(logData, (logResult) -> {
                    if (logResult == null) {
                        Logger.logError(false, TAG, "Error uploading log to server: No response from server.");
                        jobFinished(parameters, true); // No connection to the server. Try it again.
                    } else if (!logResult.isSuccess()) {
                        Logger.logError(false, TAG, "Error uploading log to server:" + logResult.getErrorMessage());
                        jobFinished(parameters, true); // Re-try to upload the log at a later time.
                    } else {
                        jobFinished(parameters, false); // Success
                    }
                });
            } catch (Exception e) {
                Logger.logError(false, TAG, "Error preparing log data for uploading to the server:", e);

                if (e instanceof JSONException) {
                    jobFinished(parameters, false); // Json-exception cannot be retried, because it will fail again.
                } else {
                    jobFinished(parameters, true);
                }
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        return true;
    }
}
