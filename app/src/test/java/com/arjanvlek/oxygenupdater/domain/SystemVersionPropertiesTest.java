package com.arjanvlek.oxygenupdater.domain;

import com.arjanvlek.oxygenupdater.internal.Utils;

import org.junit.Assert;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

abstract class SystemVersionPropertiesTest {

    private final SystemVersionProperties systemVersionProperties = new SystemVersionProperties(null, null, null, null, null, false);

    private static final String OS_OTA_VERSION_NUMBER_LOOKUP_KEY = "ro.build.version.ota";
    private static final String OS_VERSION_NUMBER_LOOKUP_KEY = "ro.oxygen.version, ro.build.ota.versionname";
    private static final String SUPPORTED_BUILD_FINGERPRINT_KEYS = "release-keys";
    private static final String BUILD_FINGERPRINT_LOOKUP_KEY = "ro.build.oemfingerprint, ro.build.fingerprint";
    private static final String DEVICE_NAME_LOOKUP_KEY = "ro.display.series, ro.build.product";
    private static final String AB_PARTITION_LAYOUT_LOOKUP_KEY = "ro.build.ab_update";

    /**
     * Test if a device is supported in the app
     * @param propertiesInDir Directory name of properties files
     * @param propertiesOfVersion OS version of build.prop file
     * @param expectedDeviceDisplayName23x Display name as shown in app 2.3.2 and newer.
     * @param expectedDeviceDisplayName12x Display name as shown in app between 1.2.0 and 2.3.1.
     * @param expectedDeviceDisplayName11x Display name as shown in app between 1.1.x and older.
     * @param expectedOxygenOs Expected OxygenOS version. Is the same as propertiesOfVersion on newer devices (and op1) or like expectedOxygenOSOta in other cases.
     * @param expectedOxygenOsOta Expected OxygenOS OTA version (as sent to the server to query for updates).
     * @return Whether or not the device is marked as supported in the app.
     */
    boolean isSupportedDevice(String propertiesInDir, String propertiesOfVersion, String expectedDeviceDisplayName23x, String expectedDeviceDisplayName12x, String expectedDeviceDisplayName11x, String expectedOxygenOs, String expectedOxygenOsOta, boolean expectedAbPartitionLayout) {
        String properties = readPropertiesFile(propertiesInDir, propertiesOfVersion);

        String deviceDisplayName23x = readProperty(DEVICE_NAME_LOOKUP_KEY, properties);
        String deviceDisplayName12x = readProperty("ro.display.series", properties);
        String deviceDisplayName11x = readProperty("ro.build.product", properties);

        String oxygenOSDisplayVersion = readProperty(OS_VERSION_NUMBER_LOOKUP_KEY, properties);
        String oxygenOSOtaVersion = readProperty(OS_OTA_VERSION_NUMBER_LOOKUP_KEY, properties);

        String buildFingerPrint = readProperty(BUILD_FINGERPRINT_LOOKUP_KEY, properties);
        boolean abPartitionLayout = Boolean.parseBoolean(readProperty(AB_PARTITION_LAYOUT_LOOKUP_KEY, properties));

        Assert.assertEquals(expectedDeviceDisplayName23x, deviceDisplayName23x);
        Assert.assertEquals(expectedDeviceDisplayName12x, deviceDisplayName12x);
        Assert.assertEquals(expectedDeviceDisplayName11x, deviceDisplayName11x);
        Assert.assertEquals(expectedOxygenOs, oxygenOSDisplayVersion);
        Assert.assertEquals(expectedOxygenOsOta, oxygenOSOtaVersion);
        Assert.assertTrue(buildFingerPrint.contains(SUPPORTED_BUILD_FINGERPRINT_KEYS));
        Assert.assertEquals(expectedAbPartitionLayout, abPartitionLayout);

        SystemVersionProperties systemVersionProperties = new SystemVersionProperties(expectedDeviceDisplayName23x, oxygenOSDisplayVersion, oxygenOSOtaVersion, null, buildFingerPrint, abPartitionLayout);
        Assert.assertTrue(Utils.isSupportedDevice(systemVersionProperties, getAllOnePlusDevices_app232AndNewer()));

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
            Method readBuildPropItem = SystemVersionProperties.class.getDeclaredMethod("readBuildPropItem", String.class, String.class, String.class);
            readBuildPropItem.setAccessible(true);

            String rawResult = (String) readBuildPropItem.invoke(systemVersionProperties, key, propertyFileContents, null);

            if(!rawResult.equals(NO_OXYGEN_OS)) {
                String[] keyValue = rawResult.split("=");
                if(keyValue.length > 1) {
                    result = keyValue[1];
                }
            }

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private List<Device> getAllOnePlusDevices_app11AndOlder() {
        Device OnePlusOne = new Device(5, "OnePlus One", "OnePlus");
        Device OnePlus2 = new Device(1, "OnePlus 2", "OnePlus2");
        Device OnePlusX = new Device(3, "OnePlus X", "OnePlus");
        Device OnePlus3 = new Device(2, "OnePlus 3", "OnePlus3");
        Device OnePlus3T = new Device(6, "OnePlus 3T", "OnePlus3");
        Device OnePlus5 = new Device(7, "OnePlus 5", "OnePlus5");
        Device OnePlus5T = new Device(8, "OnePlus 5T", "OnePlus5T");
        Device OnePlus6 = new Device(9, "OnePlus 6", "OnePlus6");
        Device OnePlus6TGlobal = new Device(10, "OnePlus 6T", "OnePlus6T");

        return Arrays.asList(OnePlus2, OnePlus3, OnePlusX, OnePlusOne, OnePlus3T, OnePlus5, OnePlus5T, OnePlus6, OnePlus6TGlobal);
    }

    private List<Device> getAllOnePlusDevices_app12Until231() {
        Device OnePlusOne = new Device(5, "OnePlus One", "OnePlus");
        Device OnePlus2 = new Device(1, "OnePlus 2", "OnePlus2");
        Device OnePlusX = new Device(3, "OnePlus X", "OnePlus X");
        Device OnePlus3 = new Device(2, "OnePlus 3", "OnePlus 3");
        Device OnePlus3T = new Device(6, "OnePlus 3T", "OnePlus 3T");
        Device OnePlus5 = new Device(7, "OnePlus 5", "OnePlus 5");
        Device OnePlus5T = new Device(8, "OnePlus 5T", "OnePlus 5T");
        Device OnePlus6 = new Device(9, "OnePlus 6", "OnePlus 6");
        Device OnePlus6TGlobal = new Device(10, "OnePlus 6T", "OnePlus 6T");

        return Arrays.asList(OnePlus2, OnePlus3, OnePlusX, OnePlusOne, OnePlus3T, OnePlus5, OnePlus5T, OnePlus6, OnePlus6TGlobal);
    }

    private List<Device> getAllOnePlusDevices_app232AndNewer() {
        Device OnePlusOne = new Device(5, "OnePlus One", "OnePlus, One");
        Device OnePlus2 = new Device(1, "OnePlus 2", "OnePlus2");
        Device OnePlusX = new Device(3, "OnePlus X", "OnePlus X");
        Device OnePlus3 = new Device(2, "OnePlus 3", "OnePlus 3");
        Device OnePlus3T = new Device(6, "OnePlus 3T", "OnePlus 3T");
        Device OnePlus5 = new Device(7, "OnePlus 5", "OnePlus 5");
        Device OnePlus5T = new Device(8, "OnePlus 5T", "OnePlus 5T");
        Device OnePlus6 = new Device(9, "OnePlus 6", "OnePlus 6");
        Device OnePlus6TGlobal = new Device(10, "OnePlus 6T", "OnePlus 6T");

        return Arrays.asList(OnePlus2, OnePlus3, OnePlusX, OnePlusOne, OnePlus3T, OnePlus5, OnePlus5T, OnePlus6, OnePlus6TGlobal);
    }
}
