package com.arjanvlek.oxygenupdater.domain;

import com.arjanvlek.oxygenupdater.internal.Utils;

import junit.framework.Assert;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

abstract class SystemVersionPropertiesTest {

    private final SystemVersionProperties systemVersionProperties = new SystemVersionProperties(null, null, null, null, null);

    /*
            buildConfigField "String", "DEVICE_NAME_LOOKUP_KEY", "\"ro.display.series\""
            buildConfigField "String", "DEVICE_CODENAME_LOOKUP_KEY", "\"ro.build.product\""
            buildConfigField "String", "OS_VERSION_NUMBER_LOOKUP_KEY_1", "\"ro.oxygen.version\""
            buildConfigField "String", "OS_VERSION_NUMBER_LOOKUP_KEY_2", "\"ro.build.ota.versionname\""
            buildConfigField "String", "OS_OTA_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.ota\""
            buildConfigField "String", "BUILD_FINGERPRINT_LOOKUP_KEY", "\"ro.build.oemfingerprint\""
            buildConfigField "String", "SUPPORTED_BUILD_FINGERPRINT_KEYS", "\"release-keys\"" // Only devices using a properly signed (a.k.a. official) version of OxygenOS are supported.
     */

    boolean isSupportedDevice(String propertiesInDir, String propertiesOfVersion, String expectedDeviceDisplayName, String expectedDeviceInternalName, String expectedOxygenOs1, String expectedOxygenOs2, String expectedOxygenOsOta) {
        String properties = readPropertiesFile(propertiesInDir, propertiesOfVersion);

        String deviceDisplayName = readProperty("ro.display.series", properties);
        String deviceInternalName = readProperty("ro.build.product", properties);

        String oxygenOSDisplayVersion1 = readProperty("ro.oxygen.version", properties);
        String oxygenOSDisplayVersion2 = readProperty("ro.build.ota.versionname", properties);
        String oxygenOSOtaVersion = readProperty("ro.build.version.ota", properties);

        String buildFingerPrint = readProperty("ro.build.oemfingerprint", properties);

        Assert.assertEquals(expectedDeviceDisplayName, deviceDisplayName);
        Assert.assertEquals(expectedDeviceInternalName, deviceInternalName);
        Assert.assertEquals(expectedOxygenOs1, oxygenOSDisplayVersion1);
        Assert.assertEquals(expectedOxygenOs2, oxygenOSDisplayVersion2);
        Assert.assertEquals(expectedOxygenOsOta, oxygenOSOtaVersion);
        Assert.assertTrue(buildFingerPrint.contains("release-keys"));

        SystemVersionProperties systemVersionProperties = new SystemVersionProperties(deviceDisplayName, !oxygenOSDisplayVersion1.equals(NO_OXYGEN_OS) ? oxygenOSDisplayVersion1 : oxygenOSDisplayVersion2, oxygenOSOtaVersion, null, buildFingerPrint);
        Assert.assertTrue(Utils.isSupportedDevice(systemVersionProperties, getAllOnePlusDevices_app12Until231()));

        return true;
    }

    private String readPropertiesFile(String deviceName, String oxygenOsVersion) {
        // Read the build.prop file from the test resources folder.
        InputStream propertiesStream = this.getClass().getResourceAsStream("/build-props/" + deviceName + "/" + oxygenOsVersion + ".prop");
        Assert.assertNotNull("Build.prop file build-props/" + deviceName + "/" + oxygenOsVersion + ".prop does not exist!", propertiesStream);

        // Convert input stream to String using Scanner method.
        Scanner scanner = new Scanner(propertiesStream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private String readProperty(String key, String propertyFileContents) {

        String result = NO_OXYGEN_OS;
        try {
            Method readBuildPropItem = SystemVersionProperties.class.getDeclaredMethod("readBuildPropItem", String.class, String.class, String.class, String.class);
            readBuildPropItem.setAccessible(true);

            BufferedReader reader = new BufferedReader(new StringReader(propertyFileContents));
            String line;

            while ((line = reader.readLine()) != null) {
                String rawResult = (String) readBuildPropItem.invoke(systemVersionProperties, result, key, line, null);

                if(!rawResult.equals(NO_OXYGEN_OS)) {
                    String[] keyValue = rawResult.split("=");
                    if(keyValue.length > 1) {
                        result = keyValue[1];
                    }
                }
            }

            return result;

        } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Device> getAllOnePlusDevices_app11AndOlder() {
        Device OnePlusOne = new Device(5, "OnePlus One", "OnePlus");
        Device OnePlus2 = new Device(1, "OnePlus 2", "OnePlus2");
        Device OnePlusX = new Device(3, "OnePlus X", "OnePlus");
        Device OnePlus3 = new Device(2, "OnePlus 3", "OnePlus3");
        Device OnePlus3T = new Device(6, "OnePlus 3T", "OnePlus3");
        Device OnePlus5 = new Device(7, "OnePlus 5", "OnePlus5");
        Device OnePlus5T = new Device(8, "OnePlus 5T", "OnePlus5T");

        return Arrays.asList(OnePlus2, OnePlus3, OnePlusX, OnePlusOne, OnePlus3T, OnePlus5, OnePlus5T);
    }

    private List<Device> getAllOnePlusDevices_app12Until231() {
        Device OnePlusOne = new Device(5, "OnePlus One", "OnePlus");
        Device OnePlus2 = new Device(1, "OnePlus 2", "OnePlus2");
        Device OnePlusX = new Device(3, "OnePlus X", "OnePlus X");
        Device OnePlus3 = new Device(2, "OnePlus 3", "OnePlus 3");
        Device OnePlus3T = new Device(6, "OnePlus 3T", "OnePlus 3T");
        Device OnePlus5 = new Device(7, "OnePlus 5", "OnePlus 5");
        Device OnePlus5T = new Device(8, "OnePlus 5T", "OnePlus 5T");

        return Arrays.asList(OnePlus2, OnePlus3, OnePlusX, OnePlusOne, OnePlus3T, OnePlus5, OnePlus5T);
    }

    private List<Device> getAllOnePlusDevices_app232AndNewer() {
        Device OnePlusOne = new Device(5, "OnePlus One", "OnePlus, One");
        Device OnePlus2 = new Device(1, "OnePlus 2", "OnePlus2");
        Device OnePlusX = new Device(3, "OnePlus X", "OnePlus X, OnePlus");
        Device OnePlus3 = new Device(2, "OnePlus 3", "OnePlus 3");
        Device OnePlus3T = new Device(6, "OnePlus 3T", "OnePlus 3T");
        Device OnePlus5 = new Device(7, "OnePlus 5", "OnePlus 5");
        Device OnePlus5T = new Device(8, "OnePlus 5T", "OnePlus 5T");

        return Arrays.asList(OnePlus2, OnePlus3, OnePlusX, OnePlusOne, OnePlus3T, OnePlus5, OnePlus5T);
    }
}
