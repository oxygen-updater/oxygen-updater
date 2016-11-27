package com.arjanvlek.oxygenupdater;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.util.Log;

import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.Support.ServerConnector;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.joda.time.LocalDateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ApplicationContext extends Application {
    private List<Device> devices;
    private LocalDateTime deviceFetchDate;
    private ServerConnector serverConnector;
    public static final String NO_OXYGEN_OS = "no_oxygen_os_ver_found";
    public static final int NUMBER_OF_INSTALL_GUIDE_PAGES = 5;

    public static final String APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME;
    public static final String LOCALE_DUTCH = "Nederlands";
    private static SystemVersionProperties SYSTEM_VERSION_PROPERTIES_INSTANCE;
    public static final String TAG = "ApplicationContext";


    // Used for Google Play Services check
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public List<Device> getDevices() {
        return getDevices(false);
    }

    /**
     * Prevents the /devices request to be performed more than once by storing it in the Application class.
     * If the stored data is more than 5 minutes old, one new request is allowed and so on for each 5 minutes.
     * @return List of Devices that are enabled on the server.
     */
    public List<Device> getDevices(boolean alwaysFetch) {
        LocalDateTime now = LocalDateTime.now();
        if(devices != null && deviceFetchDate != null && deviceFetchDate.plusMinutes(5).isAfter(now) && !alwaysFetch) {
            return devices;
        }

        else {
            if(serverConnector == null) {
                Log.v(TAG, "Created ServerConnector for use within the application...");
                serverConnector = new ServerConnector();
            }
            Log.v(TAG, "Used ServerConnector to fetch devices...");
            devices = serverConnector.getDevices();
            deviceFetchDate = LocalDateTime.now();
            return devices;
        }
    }

    public ServerConnector getServerConnector() {
        if(serverConnector == null) {
            Log.v(TAG, "Created ServerConnector for use within the application...");
            serverConnector = new ServerConnector();
            return serverConnector;
        }
        else {
            return serverConnector;
        }
    }

    public SystemVersionProperties getSystemVersionProperties() {
        if(SYSTEM_VERSION_PROPERTIES_INSTANCE == null) {
            SystemVersionProperties systemVersionProperties = new SystemVersionProperties();
            String oxygenOSVersion = NO_OXYGEN_OS;
            String oxygenOSOTAVersion = NO_OXYGEN_OS;
            String oxygenDeviceName = NO_OXYGEN_OS;
            String oemFingerprint = NO_OXYGEN_OS;
            String securityPatchDate = NO_OXYGEN_OS;
            try {
                Process getBuildPropProcess = new ProcessBuilder()
                        .command("getprop")
                        .redirectErrorStream(true)
                        .start();
                Log.v(TAG, "Started fetching device properties using 'getprop' command...");

                BufferedReader in = new BufferedReader(new InputStreamReader(getBuildPropProcess.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains("ro.build.ota.versionname")) {
                        oxygenOSVersion = inputLine.replace("[ro.build.ota.versionname]: ", "");
                        oxygenOSVersion = oxygenOSVersion.replace("[", "");
                        oxygenOSVersion = oxygenOSVersion.replace("]", "");
                        Log.v(TAG, "Found Oxygen OS ROM with version " + oxygenOSVersion + " on this device...");
                    }

                    if (inputLine.contains("ro.build.version.ota")) {
                        oxygenOSOTAVersion = inputLine.replace("[ro.build.version.ota]: ", "");
                        oxygenOSOTAVersion = oxygenOSOTAVersion.replace("[", "");
                        oxygenOSOTAVersion = oxygenOSOTAVersion.replace("]", "");
                        Log.v(TAG, "Found Oxygen OS ROM with OTA version " + oxygenOSOTAVersion + " on this device...");
                    }

                    if (inputLine.contains("ro.build.product")) {
                        oxygenDeviceName = inputLine.replace("[ro.build.product]: ", "");
                        oxygenDeviceName = oxygenDeviceName.replace("[", "");
                        oxygenDeviceName = oxygenDeviceName.replace("]", "");
                        Log.v(TAG, "Detected Oxygen OS Device: " + oxygenDeviceName + " ...");
                    }

                    if(inputLine.contains("ro.build.oemfingerprint")) {
                        oemFingerprint = inputLine.replace("[ro.build.oemfingerprint]: ", "");
                        oemFingerprint = oemFingerprint.replace("[", "");
                        oemFingerprint = oemFingerprint.replace("]", "");
                    }

                    if(securityPatchDate.equals(NO_OXYGEN_OS)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            securityPatchDate = Build.VERSION.SECURITY_PATCH;
                        } else {
                            if (inputLine.contains("ro.build.version.security_patch")) {
                                securityPatchDate = inputLine.replace("[ro.build.version.security_patch]: ", "");
                                securityPatchDate = securityPatchDate.replace("[", "");
                                securityPatchDate = securityPatchDate.replace("]", "");
                            }
                        }
                    }
                }
                getBuildPropProcess.destroy();
                Log.v(TAG, "Finished fetching device properties using 'getprop' command...");

            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
            systemVersionProperties.setOxygenDeviceName(oxygenDeviceName);
            systemVersionProperties.setOxygenOSVersion(oxygenOSVersion);
            systemVersionProperties.setOxygenOSOTAVersion(oxygenOSOTAVersion);
            systemVersionProperties.setOemFingerprint(oemFingerprint);
            systemVersionProperties.setSecurityPatchDate(securityPatchDate);
            SYSTEM_VERSION_PROPERTIES_INSTANCE = systemVersionProperties;
            return SYSTEM_VERSION_PROPERTIES_INSTANCE;
        } else {
            return SYSTEM_VERSION_PROPERTIES_INSTANCE;
        }
    }

    /**
     * Checks if the Google Play Services are installed on the device.
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
