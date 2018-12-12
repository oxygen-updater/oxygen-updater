package com.arjanvlek.oxygenupdater.domain;


import android.os.Build;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

public class SystemVersionProperties {

    private final String oxygenDeviceName;
    private final String oxygenOSVersion;
    private final String oxygenOSOTAVersion;
    private final String securityPatchDate;
    private final String oemFingerprint;
    private final boolean ABPartitionLayout;
    private static final String TAG = "SystemVersionProperties";

    public SystemVersionProperties() {
        String oxygenOSVersion = NO_OXYGEN_OS;
        String oxygenOSOTAVersion = NO_OXYGEN_OS;
        String oxygenDeviceName = NO_OXYGEN_OS;
        String oemFingerprint = NO_OXYGEN_OS;
        String securityPatchDate = NO_OXYGEN_OS;
        boolean ABPartitionLayout = false;
        try {
            Process getBuildPropProcess = Runtime.getRuntime().exec("getprop");

            Logger.logVerbose(TAG, "Started fetching device properties using 'getprop' command...");

            Scanner scanner = new Scanner(getBuildPropProcess.getInputStream()).useDelimiter("\\A");
            String properties = scanner.hasNext() ? scanner.next() : "";

            getBuildPropProcess.destroy();

            oxygenDeviceName = readBuildPropItem(BuildConfig.DEVICE_NAME_LOOKUP_KEY, properties, "Detected device: %s ...");
            oxygenOSVersion = readBuildPropItem(BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEY, properties, "Detected Oxygen OS ROM with version: %s ...");
            oxygenOSOTAVersion = readBuildPropItem(BuildConfig.OS_OTA_VERSION_NUMBER_LOOKUP_KEY, properties, "Detected Oxygen OS ROM with OTA version: %s ...");
            oemFingerprint = readBuildPropItem(BuildConfig.BUILD_FINGERPRINT_LOOKUP_KEY, properties, "Detected build fingerprint: %s ...");
            ABPartitionLayout = Boolean.parseBoolean(readBuildPropItem(BuildConfig.AB_UPDATE_LOOKUP_KEY, properties, "Device has A/B partition layout: %s ..."));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                securityPatchDate = Build.VERSION.SECURITY_PATCH;
            } else {
                securityPatchDate = readBuildPropItem("ro.build.version.security_patch", properties, "Detected security patch level: %s ...");
            }

            Logger.logVerbose(TAG, "Finished fetching device properties using 'getprop' command...");

        } catch (Exception e) {
            Logger.logError(TAG, e.getLocalizedMessage());
        }
        this.oxygenDeviceName = oxygenDeviceName;
        this.oxygenOSVersion = oxygenOSVersion;
        this.oxygenOSOTAVersion = oxygenOSOTAVersion;
        this.oemFingerprint = oemFingerprint;
        this.securityPatchDate = securityPatchDate;
        this.ABPartitionLayout = ABPartitionLayout;
    }

    public SystemVersionProperties(String oxygenDeviceName, String oxygenOSVersion, String oxygenOSOTAVersion, String securityPatchDate, String oemFingerprint, boolean ABPartitionLayout) {
        this.oxygenDeviceName = oxygenDeviceName;
        this.oxygenOSVersion = oxygenOSVersion;
        this.oxygenOSOTAVersion = oxygenOSOTAVersion;
        this.securityPatchDate = securityPatchDate;
        this.oemFingerprint = oemFingerprint;
        this.ABPartitionLayout = ABPartitionLayout;
    }

    private String readBuildPropItem(String itemKeys, String buildProperties, String logText) throws IOException {
        if (buildProperties == null || buildProperties.isEmpty()) return NO_OXYGEN_OS;

        String result = NO_OXYGEN_OS;

        String[] items = itemKeys.trim().replace(" ", "").split(",");

        for(String item : items) {

            BufferedReader in = new BufferedReader(new StringReader(buildProperties));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains(item)) {
                    result = inputLine.replace("[" + item + "]: ", "");
                    result = result.replace("[", "");
                    result = result.replace("]", "");
                    if(logText != null) Logger.logVerbose(TAG, String.format(logText, result));
                    return result; // Return the first successfully detected item. This because some keys have multiple values which all exist in the same properties file.
                }
            }
        }
        return result;
    }

    public String getOxygenDeviceName() {
        return oxygenDeviceName;
    }

    public String getOxygenOSVersion() {
        return oxygenOSVersion;
    }

    public String getOxygenOSOTAVersion() {
        return oxygenOSOTAVersion;
    }

    public String getSecurityPatchDate() {
        return securityPatchDate;
    }

    public String getOemFingerprint() {
        return oemFingerprint;
    }

    public boolean isABPartitionLayout() {
        return ABPartitionLayout;
    }
}
