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
    private List<Device> devices;
    private LocalDateTime deviceFetchDate;
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

    public void getDevices(Callback<List<Device>> callback) {
        new GetDevices(callback, false).execute();
    }

    public void getDevices(Callback<List<Device>> callback, boolean alwaysFetch) {
        new GetDevices(callback, alwaysFetch).execute();
    }

    public void getUpdateMethods(long deviceId, Callback<List<UpdateMethod>> callback) {
        new GetUpdateMethods(deviceId, callback).execute();
    }

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

    private class GetDevices extends AsyncTask<Void, Void, List<Device>> {

        private final Callback<List<Device>> callback;
        private final boolean alwaysFetch;

        public GetDevices(Callback<List<Device>> callback, boolean alwaysFetch) {
            this.callback = callback;
            this.alwaysFetch = alwaysFetch;
        }

        @Override
        protected List<Device> doInBackground(Void... params) {
            int numberOfTimes = 0;
            List<Device> devices = doGetDevices(alwaysFetch);
            if (devices == null || devices.isEmpty()) {
                while (numberOfTimes < 5) {
                    numberOfTimes++;
                    devices = doGetDevices(true);
                    if (devices != null && !devices.isEmpty()) break;
                }
            }
            return devices;
        }

        @Override
        protected void onPostExecute(List<Device> devices) {
            callback.onActionPerformed(devices);
        }
    }

    private class GetUpdateMethods extends AsyncTask<Void, Void, List<UpdateMethod>> {

        private final long deviceId;
        private final Callback<List<UpdateMethod>> callback;

        public GetUpdateMethods(long deviceId, Callback<List<UpdateMethod>> callback) {
            this.deviceId = deviceId;
            this.callback = callback;
        }

        @Override
        public List<UpdateMethod> doInBackground(Void... params) {
            return getServerConnector().getUpdateMethods(deviceId);
        }

        @Override
        public void onPostExecute(List<UpdateMethod> updateMethods) {
            callback.onActionPerformed(updateMethods);
        }
    }

    /**
     * Prevents the /devices request to be performed more than once by storing it in the Application class.
     * If the stored data is more than 5 minutes old, one new request is allowed and so on for each 5 minutes.
     * @return List of Devices that are enabled on the server.
     */
    private List<Device> doGetDevices(boolean alwaysFetch) {
        if (devices != null && deviceFetchDate != null && deviceFetchDate.plusMinutes(5).isAfter(LocalDateTime.now()) && !alwaysFetch) {
            Log.v(TAG, "Used local cache to fetch devices...");
            return devices;
        }

        else {
            Log.v(TAG, "Used ServerConnector to fetch devices...");
            devices = getServerConnector().getDevices();
            deviceFetchDate = LocalDateTime.now();
            return devices;
        }
    }
}
