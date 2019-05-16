package com.arjanvlek.oxygenupdater.domain;


import android.os.Build;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

/**
 * Contains some properties of the OS / ROM installed on the device.
 * Used to read / extract OnePlus-specific properties from the ROM.
 */
public class SystemVersionProperties {

    /** Matchable name of the device. Must be present in the Devices returned by ServerConnector */
    private final String oxygenDeviceName;
    /** Human-readable OxygenOS version. Shown within the UI of the app */
    private final String oxygenOSVersion;
    /** Technical / OTA version of OxygenOS. Used to check for updates and shown in Device Info tab */
    private final String oxygenOSOTAVersion;
    /** Security patch date. Must be looked up manually on Android versions < 6.0 */
    private final String securityPatchDate;
    /** Fingerprint of the build. Used to check if the device uses an official build of OxygenOS */
    private final String oemFingerprint;
    /**
     * Whether or not the device has an A/B partition layout.
     * Required to generate a proper install script for Automatic Update Installations (root feature)
     */
    private final boolean ABPartitionLayout;

    private static final String TAG = "SystemVersionProperties";
    private static final String SECURITY_PATCH_LOOKUP_KEY = "ro.build.version.security_patch";

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
                securityPatchDate = Build.VERSION.SECURITY_PATCH; // Already available using Android API since Android 6.0
            } else {
                securityPatchDate = readBuildPropItem(SECURITY_PATCH_LOOKUP_KEY, properties, "Detected security patch level: %s ...");
            }

            Logger.logVerbose(TAG, "Finished fetching device properties using 'getprop' command...");

        } catch (Exception e) {
            Logger.logError(TAG, e.getLocalizedMessage(), e);
        }

        this.oxygenDeviceName = oxygenDeviceName;
        this.oxygenOSVersion = oxygenOSVersion;
        this.oxygenOSOTAVersion = oxygenOSOTAVersion;
        this.oemFingerprint = oemFingerprint;
        this.securityPatchDate = securityPatchDate;
        this.ABPartitionLayout = ABPartitionLayout;
    }

    // Only called from within the tests, as we do not want to call the real 'getprop' command from there.
    public SystemVersionProperties(String oxygenDeviceName, String oxygenOSVersion, String oxygenOSOTAVersion, String securityPatchDate, String oemFingerprint, boolean ABPartitionLayout) {
        System.out.println("Warning: SystemVersionProperties was constructed using a debug constructor. This should only happen during unit tests!");
        this.oxygenDeviceName = oxygenDeviceName;
        this.oxygenOSVersion = oxygenOSVersion;
        this.oxygenOSOTAVersion = oxygenOSOTAVersion;
        this.securityPatchDate = securityPatchDate;
        this.oemFingerprint = oemFingerprint;
        this.ABPartitionLayout = ABPartitionLayout;
    }

    private String readBuildPropItem(String itemKeys, String buildProperties, String logText) throws IOException {
        if (buildProperties == null || buildProperties.isEmpty()) {
            return NO_OXYGEN_OS;
        }

        String result = NO_OXYGEN_OS;

        // Some keys are not present on all devices. Therefore, we'll need support for multiple keys in a single string.
        // If the first key is not present on this device, try the next key. We split the key string by ", "
        String[] items = itemKeys
                .trim()
                .replace(" ", "")
                .split(",");

        for (String item : items) {

            BufferedReader in = new BufferedReader(new StringReader(buildProperties));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains(item)) {
                    // Remove brackets ([ and ]) and ":" from the getprop command output line
                    result = inputLine.replace("[" + item + "]: ", "");
                    result = result.replace("[", "");
                    result = result.replace("]", "");

                    // @hack #1 (OS_VERSION_NUMBER_LOOKUP_KEY): OxygenOS 2.0.0 - 3.x sometimes contain incorrect H2OS values for ro.rom.version
                    // if this is the case, discard the value and try with the next item in "items" array
                    if (item.equals("ro.rom.version") && result.contains("H2OS")) {
                        result = NO_OXYGEN_OS;
                        continue;
                    }

                    // @hack #2 (OS_VERSION_NUMBER_LOOKUP_KEY): OnePlus 7 and later store hardcoded "Oxygen OS " in their version number of the firmware.
                    // As the app only shows the number or ads custom formatting, remove this prefix
                    if (item.equals("ro.rom.version") && result.contains("Oxygen OS ")) {
                        result = result.replace("Oxygen OS ", "");
                    }

                    // @hack #3 (DEVICE_NAME_LOOKUP_KEY / OnePlus 7 Pro Support): OnePlus 7 Pro comes in regional variants which cannot be detected by ro.display.series.
                    // However, its alternative (ro.product.name) does not play nice with values present on older devices.
                    // Bypass: if the key is 'ro.display.series' and the value is 'OnePlus 7 Pro', then read 'ro.product.name' instead to detect the correct device
                    if (item.equals("ro.display.series") && result.equals("OnePlus 7 Pro")) {
                        // Android Logger class is not loaded during unit tests, so omit logging if called from test.
                        String logMessage = logText != null ? "Detected OnePlus 7 Pro variant: %s" : null;
                        result = readBuildPropItem("ro.product.name", buildProperties, logMessage);
                    }

                    if (logText != null) {
                        Logger.logVerbose(TAG, String.format(logText, result));
                    }

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
