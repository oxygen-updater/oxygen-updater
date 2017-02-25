package com.arjanvlek.oxygenupdater;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.Server.ServerConnector;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

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

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public ServerConnector getServerConnector() {
        if (serverConnector == null) {
            Log.v(TAG, "Created ServerConnector for use within the application...");
            serverConnector = new ServerConnector(new SettingsManager(this));
            return serverConnector;
        } else {
            return serverConnector;
        }
    }

    public SystemVersionProperties getSystemVersionProperties() {
        // Store the system version properties in a cache, to prevent unnecessary calls to the native "getProp" command.
        if (systemVersionProperties == null) {
            Log.v(TAG, "Creating new SystemVersionProperties instance...");
            systemVersionProperties = new SystemVersionProperties();
            return systemVersionProperties;
        } else {
            Log.v(TAG, "Using cached instance of SystemVersionProperties");
            return systemVersionProperties;
        }
    }


    /**
     * Checks if the Google Play Services are installed on the device.
     *
     * @return Returns if the Google Play Services are installed.
     */
    public boolean checkPlayServices(Activity activity, boolean showErrorIfMissing) {
        Log.v(TAG, "Executing Google Play Services check...");
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS && showErrorIfMissing) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(activity, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                System.exit(0);
            }
            Log.v(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!");
            return false;
        } else {
            boolean result = resultCode == ConnectionResult.SUCCESS;
            if(result) Log.v(TAG, "Google Play Services are available.");
            else Log.v(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!");
            return result;
        }
    }
}
