package com.arjanvlek.oxygenupdater.Support;

import android.os.AsyncTask;
import android.os.Build;

import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;

import java.util.List;

import java8.util.Optional;
import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.ApplicationContext.NO_OXYGEN_OS;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS;

public class SupportedDeviceManager extends AsyncTask<Void, Void, List<Device>> {

    private SupportedDeviceCallback callback;
    private ApplicationContext applicationContext;

    public SupportedDeviceManager(Object callback, ApplicationContext applicationContext) {
        try {
            this.callback = (SupportedDeviceCallback) callback;
        } catch (ClassCastException e) {
            this.callback = null;
        }
        this.applicationContext = applicationContext;
    }

    @Override
    protected List<Device> doInBackground(Void... params) {
        return applicationContext.getDevices();
    }

    @Override
    protected void onPostExecute(List<Device> devices) {


        boolean deviceIsSupported = isSupportedDevice(devices);

        if(deviceIsSupported) { // To prevent unnecessary device checks.
            SettingsManager settingsManager = new SettingsManager(applicationContext.getApplicationContext());
            settingsManager.savePreference(PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, true);
        }

        if(callback != null) {
            callback.displayUnsupportedMessage(deviceIsSupported);
        }
    }

    private boolean isSupportedDevice(List<Device> devices) {
        SystemVersionProperties systemVersionProperties = applicationContext.getSystemVersionProperties();
        String oemFingerPrint = systemVersionProperties.getOemFingerprint();
        String oxygenOsVersion = systemVersionProperties.getOxygenOSVersion();

        if(devices == null || devices.isEmpty()) {
            return oemFingerPrint != null && !oemFingerPrint.equals(NO_OXYGEN_OS) && oemFingerPrint.contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS) && oxygenOsVersion != null && !oxygenOsVersion.equals(NO_OXYGEN_OS);
            // To prevent false positives on empty server response. This still checks if official ROM is used and if an oxygen os version is found on the device.
        }

        Optional<Device> supportedDevice = StreamSupport.stream(devices)
                .filter(d -> d.getProductName() != null && d.getProductName().equals(systemVersionProperties.getOxygenDeviceName()))
                .filter(d -> d.getChipSet().equals("NOT_SET") || d.getChipSet().equals(Build.BOARD))
                .findAny();

        return supportedDevice.isPresent() && oemFingerPrint != null && !oemFingerPrint.equals(NO_OXYGEN_OS) && oemFingerPrint.contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS);
    }
}
