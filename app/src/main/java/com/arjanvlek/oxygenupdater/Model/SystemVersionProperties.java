package com.arjanvlek.oxygenupdater.Model;

import android.os.Build;

import java.util.List;

import static com.arjanvlek.oxygenupdater.ApplicationContext.NO_OXYGEN_OS;

public class SystemVersionProperties {

    private String oxygenDeviceName;
    private String oxygenOSVersion;
    private String oxygenOSOTAVersion;
    private String securityPatchDate;
    private String modelNumber;

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

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public boolean isSupportedDevice(List<Device> devices) {
        boolean supported = false;

        if(devices == null || devices.isEmpty()) {
            return Build.TAGS.contains("release-keys") && getOxygenOSVersion() != null && !getOxygenOSVersion().equals(NO_OXYGEN_OS);
            // To bypass false positives on empty server response. This still checks if official ROM is used and if an oxygen os version is found on the device.
        }

        for(Device device : devices) {
            if(device.getProductName() != null && device.getProductName().equals(getOxygenDeviceName()) && Build.TAGS.contains("release-keys")) {
                supported = true;
                break;
            }
        }
        return supported;
    }
}
