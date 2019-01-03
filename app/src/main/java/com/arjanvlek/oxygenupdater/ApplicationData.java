package com.arjanvlek.oxygenupdater;

import android.app.Activity;
import android.app.Application;

import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.views.AbstractFragment.ADS_TEST_DEVICES;

public class ApplicationData extends Application {

    private ServerConnector serverConnector;
    private SystemVersionProperties systemVersionProperties;

    public static final String NO_OXYGEN_OS = "no_oxygen_os_ver_found";
    public static final int NUMBER_OF_INSTALL_GUIDE_PAGES = 5;
    public static final String DEVICE_TOPIC_PREFIX = "device_";
    public static final String UPDATE_METHOD_TOPIC_PREFIX = "_update-method_";
    public static final String APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME;
    public static final String LOCALE_DUTCH = "Nederlands";
    private static final String TAG = "ApplicationData";
    public static final String UNABLE_TO_FIND_A_MORE_RECENT_BUILD = "unable to find a more recent build";
    public static final String NETWORK_CONNECTION_ERROR = "NETWORK_CONNECTION_ERROR";
    public static final String SERVER_MAINTENANCE_ERROR = "SERVER_MAINTENANCE_ERROR";
    public static final String APP_OUTDATED_ERROR = "APP_OUTDATED_ERROR";
    public static final String PUSH_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications";
    public static final String PROGRESS_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.progress";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private Thread.UncaughtExceptionHandler defaultExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Logger.init(this);

            // Set a global exception handler which logs all exceptions to the server, if possible.
            // Afterwards, throw the exception to crash the application (these errors can't be prevented anyway).
            SettingsManager settingsManager = new SettingsManager(this);

            if (settingsManager.getPreference(SettingsManager.PROPERTY_UPLOAD_LOGS, true)) {
                defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

                Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                    Logger.logApplicationCrash(this, e);
                    // Pass exception through the chain, which will put up "Unfortunately, Oxygen Updater has stopped" message.
                    if (defaultExceptionHandler != null) {
                        defaultExceptionHandler.uncaughtException(t, e);
                    } else {
                        // If the default handler is unavailable, force-quit the app to prevent it getting stuck.
                        System.exit(1);
                    }
                });
            }
        } catch (Exception e) {
            Logger.logError(false, TAG, "Failed to set up logger: ", e);
        }

        PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
                .setDatabaseEnabled(true)
                .setUserAgent(APP_USER_AGENT)
                .setConnectTimeout(30_000)
                .setReadTimeout(120_000)
                .build();

        PRDownloader.initialize(getApplicationContext(), config);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Logger.context = null;
    }

    public ServerConnector getServerConnector() {
        if (serverConnector == null) {
            Logger.logVerbose(TAG, "Created ServerConnector for use within the application...");
            serverConnector = new ServerConnector(new SettingsManager(this));
        }
        return serverConnector;
    }

    public SystemVersionProperties getSystemVersionProperties() {
        // Store the system version properties in a cache, to prevent unnecessary calls to the native "getProp" command.
        if (systemVersionProperties == null) {
            Logger.logVerbose(TAG, "Creating new SystemVersionProperties instance...");
            systemVersionProperties = new SystemVersionProperties(true);
        } else {
            Logger.logVerbose(false, TAG, "Using cached instance of SystemVersionProperties");
        }
        return systemVersionProperties;
    }

    public AdRequest buildAdRequest() {
        AdRequest.Builder adRequest = new AdRequest.Builder();

        StreamSupport.stream(ADS_TEST_DEVICES).forEach(adRequest::addTestDevice);
        return adRequest.build();
    }


    /**
     * Checks if the Google Play Services are installed on the device.
     *
     * @return Returns if the Google Play Services are installed.
     */
    public boolean checkPlayServices(Activity activity, boolean showErrorIfMissing) {
        Logger.logVerbose(TAG, "Executing Google Play Services check...");
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS && showErrorIfMissing) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(activity, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                System.exit(0);
            }
            Logger.logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!");
            return false;
        } else {
            boolean result = resultCode == ConnectionResult.SUCCESS;
            if (result) Logger.logVerbose(TAG, "Google Play Services are available.");
            else Logger.logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!");
            return result;
        }
    }
}
