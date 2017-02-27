package com.arjanvlek.oxygenupdater.Model;


import android.os.Build;
import android.support.annotation.NonNull;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.support.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

public class SystemVersionProperties {

    private final String oxygenDeviceName;
    private final String oxygenOSVersion;
    private final String oxygenOSOTAVersion;
    private final String securityPatchDate;
    private final boolean uploadLog;
    private final String oemFingerprint;
    private static final String TAG = "SystemVersionProperties";

    public SystemVersionProperties(boolean uploadLog) {
        this.uploadLog = uploadLog;
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
            Logger.logVerbose(uploadLog, TAG, "Started fetching device properties using 'getprop' command...");

            BufferedReader in = new BufferedReader(new InputStreamReader(getBuildPropProcess.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                oxygenDeviceName = readBuildPropItem(oxygenDeviceName, BuildConfig.DEVICE_CODENAME_LOOKUP_KEY, inputLine, "Detected Oxygen OS Device: %s ...");
                oxygenOSVersion = readBuildPropItem(oxygenOSVersion, BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEY, inputLine, "Detected Oxygen OS ROM with version %s ...");
                oxygenOSOTAVersion = readBuildPropItem(oxygenOSOTAVersion, BuildConfig.OS_OTA_VERSION_NUMBER_LOOKUP_KEY, inputLine, "Detected Oxygen OS ROM with OTA version %s ...");
                oemFingerprint = readBuildPropItem(oemFingerprint, BuildConfig.BUILD_FINGERPRINT_LOOKUP_KEY, inputLine, "Detected build fingerprint: %s ...");

                if(securityPatchDate.equals(NO_OXYGEN_OS)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        securityPatchDate = Build.VERSION.SECURITY_PATCH;
                    } else {
                        securityPatchDate = readBuildPropItem(securityPatchDate, "ro.build.version.security_patch", inputLine, "Detected security patch level of %s ...");
                    }
                }
            }
            getBuildPropProcess.destroy();
            Logger.logVerbose(uploadLog, TAG, "Finished fetching device properties using 'getprop' command...");

        } catch (IOException e) {
            Logger.logError(uploadLog, TAG, e.getLocalizedMessage());
        }
        this.oxygenDeviceName = oxygenDeviceName;
        this.oxygenOSVersion = oxygenOSVersion;
        this.oxygenOSOTAVersion = oxygenOSOTAVersion;
        this.oemFingerprint = oemFingerprint;
        this.securityPatchDate = securityPatchDate;
    }

    private String readBuildPropItem(@NonNull String result, String itemKey, String inputLine, String logText) {
        if (inputLine == null) return result;

        if (inputLine.contains(itemKey)) {
            result = inputLine.replace("[" + itemKey + "]: ", "");
            result = result.replace("[", "");
            result = result.replace("]", "");
            if(logText != null) Logger.logVerbose(uploadLog, TAG, String.format(logText, result));
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
}
