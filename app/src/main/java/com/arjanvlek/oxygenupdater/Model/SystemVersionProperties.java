package com.arjanvlek.oxygenupdater.Model;


import com.arjanvlek.oxygenupdater.BuildConfig;

import java.util.List;

import static com.arjanvlek.oxygenupdater.ApplicationContext.NO_OXYGEN_OS;

public class SystemVersionProperties {

    private String oxygenDeviceName;
    private String oxygenOSVersion;
    private String oxygenOSOTAVersion;
    private String securityPatchDate;
    private String oemFingerprint;

    public String getOxygenDeviceName() {
        return oxygenDeviceName;
    }

    public void setOxygenDeviceName(String oxygenDeviceName) {
        this.oxygenDeviceName = oxygenDeviceName;
    }

    public String getOxygenOSVersion() {
        return oxygenOSVersion;
    }

    public void setOxygenOSVersion(String oxygenOSVersion) {
        this.oxygenOSVersion = oxygenOSVersion;
    }

    public String getOxygenOSOTAVersion() {
        return oxygenOSOTAVersion;
    }

    public void setOxygenOSOTAVersion(String oxygenOSOTAVersion) {
        this.oxygenOSOTAVersion = oxygenOSOTAVersion;
    }

    public String getSecurityPatchDate() {
        return securityPatchDate;
    }

    public void setSecurityPatchDate(String securityPatchDate) {
        this.securityPatchDate = securityPatchDate;
    }

    public String getOemFingerprint() {
        return oemFingerprint;
    }

    public void setOemFingerprint(String oemFingerprint) {
        this.oemFingerprint = oemFingerprint;
    }

    public boolean isSupportedDevice(List<Device> devices) {
        boolean supported = false;

        if(devices == null || devices.isEmpty()) {
            return getOemFingerprint() != null && !getOemFingerprint().equals(NO_OXYGEN_OS) && getOemFingerprint().contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS) && getOxygenOSVersion() != null && !getOxygenOSVersion().equals(NO_OXYGEN_OS);
            // To bypass false positives on empty server response. This still checks if official ROM is used and if an oxygen os version is found on the device.
        }

        for(Device device : devices) {
            if(device.getProductName() != null && device.getProductName().equals(getOxygenDeviceName()) && getOemFingerprint() != null && !getOemFingerprint().equals(NO_OXYGEN_OS) && getOemFingerprint().contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS)) {
                supported = true;
                break;
            }
        }
        return supported;
    }
}
