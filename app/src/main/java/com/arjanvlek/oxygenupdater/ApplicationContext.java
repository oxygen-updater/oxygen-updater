package com.arjanvlek.oxygenupdater;

import android.app.Activity;
import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.Model.UpdateMethod;
import com.arjanvlek.oxygenupdater.Server.ServerConnector;
import com.arjanvlek.oxygenupdater.Support.Callback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.joda.time.LocalDateTime;

import java.util.List;

public class ApplicationContext extends Application {

    private ServerConnector serverConnector;
    private SystemVersionProperties systemVersionProperties;

    public static final String NO_OXYGEN_OS = "no_oxygen_os_ver_found";
    public static final int NUMBER_OF_INSTALL_GUIDE_PAGES = 5;
    public static final String DEVICE_TOPIC_PREFIX = "device_";
    public static final String UPDATE_METHOD_TOPIC_PREFIX = "_update-method_";
    public static final String APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME;
    public static final String LOCALE_DUTCH = "Nederlands";
    private static final String TAG = "ApplicationContext";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public ServerConnector getServerConnector() {
        if (serverConnector == null) {
            Log.v(TAG, "Created ServerConnector for use within the application...");
            serverConnector = new ServerConnector();
            return serverConnector;
        } else {
            return serverConnector;
        }
    }

    public SystemVersionProperties getSystemVersionProperties() {
        // Store the system version properties in a cache, to prevent unnecessary calls to the native "getProp" command.
        if (systemVersionProperties == null) {
            systemVersionProperties = new SystemVersionProperties();
            return systemVersionProperties;
        } else {
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
            return false;
        } else {
            return resultCode == ConnectionResult.SUCCESS;
        }
    }
}
