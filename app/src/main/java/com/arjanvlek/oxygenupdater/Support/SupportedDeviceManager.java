package com.arjanvlek.oxygenupdater.Support;

import android.os.Build;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;

import java.util.List;

import java8.util.Optional;
import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

public class SupportedDeviceManager {

    public static boolean isSupportedDevice(SystemVersionProperties systemVersionProperties, List<Device> devices) {
        String oemFingerPrint = systemVersionProperties.getOemFingerprint();
        String oxygenOsVersion = systemVersionProperties.getOxygenOSVersion();

        boolean firmwareIsSupported =
                        oemFingerPrint != null
                        && !oemFingerPrint.equals(NO_OXYGEN_OS)
                        && oemFingerPrint.contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS)
                        && oxygenOsVersion != null
                        && !oxygenOsVersion.equals(NO_OXYGEN_OS);

        if(devices == null || devices.isEmpty()) {
            // To prevent incorrect results on empty server response. This still checks if official ROM is used and if an oxygen os version is found on the device.
            return firmwareIsSupported;
        }

        Optional<Device> supportedDevice = StreamSupport.stream(devices)
                .filter(d -> d.getProductName() != null && d.getProductName().equals(systemVersionProperties.getOxygenDeviceName()))
                .filter(d -> d.getChipSet()!= null && d.getChipSet().equals(Build.BOARD))
                .findAny();

        return supportedDevice.isPresent() && firmwareIsSupported;
    }
}
