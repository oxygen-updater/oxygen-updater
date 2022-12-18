package com.oxygenupdater.models

import android.os.Build
import com.oxygenupdater.utils.Utils.checkDeviceOsSpec
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

abstract class SystemVersionPropertiesTest {

    private val systemVersionProperties = com.oxygenupdater.models.SystemVersionProperties(
        null,
        null,
        null,
        null,
        null,
    )

    /**
     * Test if a device is supported in the app
     *
     * @param propertiesInDir              Directory name of properties files
     * @param propertiesOfVersion          OS version of build.prop file or 'getprop' command output file
     * @param expectedDeviceDisplayName23x Display name as shown in app 2.3.2 and newer.
     * @param expectedDeviceDisplayName12x Display name as shown in app between 1.2.0 and 2.3.1.
     * @param expectedDeviceDisplayName11x Display name as shown in app between 1.1.x and older.
     * @param expectedOxygenOs             Expected OxygenOS version. Is the same as propertiesOfVersion on newer devices (and op1) or like expectedOxygenOSOta in other cases.
     * @param expectedOxygenOsOta          Expected OxygenOS OTA version (as sent to the server to query for updates).
     * @param expectedAbPartitionLayout    Expected value for if the device has an A/B partition layout (true) or classic partition layout (false)
     *
     * @return Whether or not the device is marked as supported in the app.
     */
    fun isSupportedDevice(
        propertiesInDir: String,
        propertiesOfVersion: String,
        expectedDeviceDisplayName23x: String?,
        expectedDeviceDisplayName12x: String?,
        expectedDeviceDisplayName11x: String?,
        expectedOxygenOs: String?,
        expectedOxygenOsOta: String?,
        expectedAbPartitionLayout: Boolean,
    ): Boolean {
        val testDataSet = readBuildPropFile(propertiesInDir, propertiesOfVersion)
        val testDataType = testDataSet.first
        val properties = testDataSet.second
        val deviceDisplayName23x = readProperty(DEVICE_NAME_LOOKUP_KEY, properties, testDataType)
        val deviceDisplayName12x = readProperty("ro.display.series", properties, testDataType)
        val deviceDisplayName11x = readProperty("ro.build.product", properties, testDataType)
        val oxygenOSDisplayVersion = readProperty(OS_VERSION_NUMBER_LOOKUP_KEY, properties, testDataType)
        val oxygenOSOtaVersion = readProperty(OS_OTA_VERSION_NUMBER_LOOKUP_KEY, properties, testDataType)
        val buildFingerPrint = readProperty(BUILD_FINGERPRINT_LOOKUP_KEY, properties, testDataType)
        val abPartitionLayout = java.lang.Boolean.parseBoolean(readProperty(AB_PARTITION_LAYOUT_LOOKUP_KEY, properties, testDataType))

        assertEquals(expectedDeviceDisplayName23x, deviceDisplayName23x)
        assertEquals(expectedDeviceDisplayName12x, deviceDisplayName12x)
        assertEquals(expectedDeviceDisplayName11x, deviceDisplayName11x)
        assertEquals(expectedOxygenOs, oxygenOSDisplayVersion)
        assertEquals(expectedOxygenOsOta, oxygenOSOtaVersion)
        assertTrue(buildFingerPrint.contains(SUPPORTED_BUILD_FINGERPRINT_KEYS))
        assertEquals(expectedAbPartitionLayout, abPartitionLayout)

        val deviceOsSpec = checkDeviceOsSpec(allOnePlusDevicesApp232AndNewer)

        assertSame(deviceOsSpec, DeviceOsSpec.SUPPORTED_OXYGEN_OS)

        return true
    }

    fun getSupportedDevice(propertiesInDir: String, propertiesOfVersion: String): Device {
        val testDataSet = readBuildPropFile(propertiesInDir, propertiesOfVersion)
        val testDataType = testDataSet.first
        val properties = testDataSet.second
        val deviceDisplayName23x = readProperty(DEVICE_NAME_LOOKUP_KEY, properties, testDataType)

        return allOnePlusDevicesApp232AndNewer
            .find { it.productNames.contains(deviceDisplayName23x) }
            ?: throw IllegalArgumentException("Unsupported device")
    }

    private fun readBuildPropFile(
        deviceName: String,
        oxygenOsVersion: String,
    ): Pair<TestDataType, String> {
        // Read the build.prop file from the test resources folder.
        var testDataType = TestDataType.BUILD_PROP_FILE
        var propertiesStream = javaClass.getResourceAsStream("/build-props/$deviceName/$oxygenOsVersion.prop")

        if (propertiesStream == null) {
            propertiesStream = javaClass.getResourceAsStream("/build-props/$deviceName/$oxygenOsVersion.getprop")
            testDataType = TestDataType.GETPROP_COMMAND_OUTPUT
        }

        assertNotNull(propertiesStream, "Test data file build-props/$deviceName/$oxygenOsVersion.(get)prop does not exist!")

        // Convert input stream to String using Scanner method.
        val scanner = Scanner(propertiesStream).useDelimiter("\\A")

        return Pair(testDataType, if (scanner.hasNext()) scanner.next() else "")
    }

    private fun readProperty(key: String, testData: String, testDataType: TestDataType): String {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        return when (testDataType) {
            TestDataType.BUILD_PROP_FILE -> getBuildPropItem(key, testData)
            TestDataType.GETPROP_COMMAND_OUTPUT -> getItemFromGetPropCommandOutput(key, testData)
            else -> throw IllegalStateException("Unknown test data type $testDataType")
        }
    }

    // This version is a bit different than on a real device. Since we parse build.prop file directly,
    // the output is "key=value" in contrast to getprop output of format "[key]:[value]".
    // So we need to split the result on the "=" sign in this helper method to get the same result as on a real device.
    private fun getBuildPropItem(key: String, propertyFileContents: String): String {
        var result = Build.UNKNOWN

        try {
            val readBuildPropItem = SystemVersionProperties::class.java.getDeclaredMethod("readBuildPropItem", String::class.java, String::class.java, String::class.java)

            readBuildPropItem.isAccessible = true

            val rawResult = readBuildPropItem.invoke(systemVersionProperties, key, propertyFileContents, null) as String

            if (rawResult != Build.UNKNOWN) {
                val keyValue = rawResult.split("=")

                if (keyValue.size > 1) {
                    result = keyValue[1]
                }
            }
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        }

        return result
    }

    // This version is the same as what happens on the real device: Parse the output from 'getprop' command to a property item.
    private fun getItemFromGetPropCommandOutput(key: String, getPropCommandOutput: String): String {
        return try {
            val readBuildPropItem = SystemVersionProperties::class.java.getDeclaredMethod("readBuildPropItem", String::class.java, String::class.java, String::class.java)

            readBuildPropItem.isAccessible = true
            readBuildPropItem.invoke(systemVersionProperties, key, getPropCommandOutput, null) as String
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        }
    }

    // do *not* add new devices here, it is pointless to support app versions from 2016-2017 on them \\
    private val allOnePlusDevicesApp11AndOlder = listOf(
        Device(5, "OnePlus One", "OnePlus"),
        Device(1, "OnePlus 2", "OnePlus2"),
        Device(3, "OnePlus X", "OnePlus"),
        Device(2, "OnePlus 3", "OnePlus3"),
        Device(6, "OnePlus 3T", "OnePlus3"),
        Device(7, "OnePlus 5", "OnePlus5"),
        Device(8, "OnePlus 5T", "OnePlus5T"),
        Device(9, "OnePlus 6", "OnePlus6"),
        Device(10, "OnePlus 6T (Global)", "OnePlus6T")
    )

    // do *not* add new devices here, it is pointless to support app versions from 2016-2017 on them \\
    private val allOnePlusDevicesApp12Until231 = listOf(
        Device(5, "OnePlus One", "OnePlus"),
        Device(1, "OnePlus 2", "OnePlus2"),
        Device(3, "OnePlus X", "OnePlus"),
        Device(2, "OnePlus 3", "OnePlus3"),
        Device(6, "OnePlus 3T", "OnePlus3"),
        Device(7, "OnePlus 5", "OnePlus5"),
        Device(8, "OnePlus 5T", "OnePlus5T"),
        Device(9, "OnePlus 6", "OnePlus6"),
        Device(10, "OnePlus 6T (Global)", "OnePlus6T")
    )

    private val allOnePlusDevicesApp232AndNewer = listOf(
        Device(5, "OnePlus One", "OnePlus, One"),
        Device(1, "OnePlus 2", "OnePlus2"),
        Device(3, "OnePlus X", "OnePlus X"),
        Device(2, "OnePlus 3", "OnePlus 3"),
        Device(6, "OnePlus 3T", "OnePlus 3T"),
        Device(7, "OnePlus 5", "OnePlus 5"),
        Device(8, "OnePlus 5T", "OnePlus 5T"),
        Device(9, "OnePlus 6", "OnePlus 6"),
        Device(10, "OnePlus 6T (Global)", "OnePlus 6T"),
        Device(11, "OnePlus 7", "OnePlus7"),
        Device(11, "OnePlus 7 Pro (EU)", "OnePlus7Pro_EEA"),
        Device(12, "OnePlus 7 Pro", "OnePlus7Pro"),
        Device(13, "OnePlus 7 Pro 5G (EU)", "OnePlus7ProNR_EEA"),
        Device(11, "OnePlus 7T (EU)", "OnePlus7T_EEA"),
        Device(12, "OnePlus 7T", "OnePlus7T"),
        Device(13, "OnePlus 7T (India)", "OnePlus7T_IN"),
        Device(11, "OnePlus 7T Pro (EU)", "OnePlus7TPro_EEA"),
        Device(12, "OnePlus 7T Pro", "OnePlus7TPro"),
        Device(13, "OnePlus 7T Pro (India)", "OnePlus7TPro_IN")
    )

    private enum class TestDataType {
        BUILD_PROP_FILE,
        GETPROP_COMMAND_OUTPUT
    }

    private class Pair<F, S>(val first: F, val second: S)

    companion object {
        private const val OS_OTA_VERSION_NUMBER_LOOKUP_KEY = "ro.build.version.ota"
        private const val OS_VERSION_NUMBER_LOOKUP_KEY = "ro.rom.version, ro.oxygen.version, ro.build.ota.versionname"
        private const val SUPPORTED_BUILD_FINGERPRINT_KEYS = "release-keys"
        private const val BUILD_FINGERPRINT_LOOKUP_KEY = "ro.build.oemfingerprint, ro.build.fingerprint"
        private const val DEVICE_NAME_LOOKUP_KEY = "ro.display.series, ro.build.product"
        private const val AB_PARTITION_LAYOUT_LOOKUP_KEY = "ro.build.ab_update"
    }
}
