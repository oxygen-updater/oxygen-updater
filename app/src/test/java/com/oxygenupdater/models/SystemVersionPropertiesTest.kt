package com.oxygenupdater.models

import android.os.Build
import com.oxygenupdater.utils.Utils.checkDeviceOsSpec
import java.lang.reflect.InvocationTargetException
import java.util.Scanner
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
        val deviceDisplayName23x = readProperty(DeviceNameLookupKey, properties, testDataType)
        val deviceDisplayName12x = readProperty("ro.display.series", properties, testDataType)
        val deviceDisplayName11x = readProperty("ro.build.product", properties, testDataType)
        val oxygenOSDisplayVersion = readProperty(OsVersionNumberLookupKey, properties, testDataType)
        val oxygenOSOtaVersion = readProperty(OsOtaVersionNumberLookupKey, properties, testDataType)
        val buildFingerPrint = readProperty(BuildFingerprintLookupKey, properties, testDataType)
        val abPartitionLayout = readProperty(AbPartitionLayoutLookupKey, properties, testDataType).toBoolean()

        assertEquals(expectedDeviceDisplayName23x, deviceDisplayName23x)
        assertEquals(expectedDeviceDisplayName12x, deviceDisplayName12x)
        assertEquals(expectedDeviceDisplayName11x, deviceDisplayName11x)
        assertEquals(expectedOxygenOs, oxygenOSDisplayVersion)
        assertEquals(expectedOxygenOsOta, oxygenOSOtaVersion)
        assertTrue(buildFingerPrint.contains(SupportedBuildFingerprintKeys))
        assertEquals(expectedAbPartitionLayout, abPartitionLayout)

        val deviceOsSpec = checkDeviceOsSpec(allOnePlusDevicesApp232AndNewer)

        assertSame(deviceOsSpec, DeviceOsSpec.SUPPORTED_OXYGEN_OS)

        return true
    }

    fun getSupportedDevice(propertiesInDir: String, propertiesOfVersion: String): Device {
        val testDataSet = readBuildPropFile(propertiesInDir, propertiesOfVersion)
        val testDataType = testDataSet.first
        val properties = testDataSet.second
        val deviceDisplayName23x = readProperty(DeviceNameLookupKey, properties, testDataType)

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

        return testDataType to if (scanner.hasNext()) scanner.next() else ""
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
        Device(5, "OnePlus One", listOf("OnePlus"), true),
        Device(1, "OnePlus 2", listOf("OnePlus2"), true),
        Device(3, "OnePlus X", listOf("OnePlus"), true),
        Device(2, "OnePlus 3", listOf("OnePlus3"), true),
        Device(6, "OnePlus 3T", listOf("OnePlus3"), true),
        Device(7, "OnePlus 5", listOf("OnePlus5"), true),
        Device(8, "OnePlus 5T", listOf("OnePlus5T"), true),
        Device(9, "OnePlus 6", listOf("OnePlus6"), true),
        Device(10, "OnePlus 6T (Global)", listOf("OnePlus6T"), true)
    )

    // do *not* add new devices here, it is pointless to support app versions from 2016-2017 on them \\
    private val allOnePlusDevicesApp12Until231 = listOf(
        Device(5, "OnePlus One", listOf("OnePlus"), true),
        Device(1, "OnePlus 2", listOf("OnePlus2"), true),
        Device(3, "OnePlus X", listOf("OnePlus"), true),
        Device(2, "OnePlus 3", listOf("OnePlus3"), true),
        Device(6, "OnePlus 3T", listOf("OnePlus3"), true),
        Device(7, "OnePlus 5", listOf("OnePlus5"), true),
        Device(8, "OnePlus 5T", listOf("OnePlus5T"), true),
        Device(9, "OnePlus 6", listOf("OnePlus6"), true),
        Device(10, "OnePlus 6T (Global)", listOf("OnePlus6T"), true)
    )

    private val allOnePlusDevicesApp232AndNewer = listOf(
        Device(5, "OnePlus One", listOf("OnePlus, One"), true),
        Device(1, "OnePlus 2", listOf("OnePlus2"), true),
        Device(3, "OnePlus X", listOf("OnePlus X"), true),
        Device(2, "OnePlus 3", listOf("OnePlus 3"), true),
        Device(6, "OnePlus 3T", listOf("OnePlus 3T"), true),
        Device(7, "OnePlus 5", listOf("OnePlus 5"), true),
        Device(8, "OnePlus 5T", listOf("OnePlus 5T"), true),
        Device(9, "OnePlus 6", listOf("OnePlus 6"), true),
        Device(10, "OnePlus 6T (Global)", listOf("OnePlus 6T"), true),
        Device(11, "OnePlus 7", listOf("OnePlus7"), true),
        Device(11, "OnePlus 7 Pro (EU)", listOf("OnePlus7Pro_EEA"), true),
        Device(12, "OnePlus 7 Pro", listOf("OnePlus7Pro"), true),
        Device(13, "OnePlus 7 Pro 5G (EU)", listOf("OnePlus7ProNR_EEA"), true),
        Device(11, "OnePlus 7T (EU)", listOf("OnePlus7T_EEA"), true),
        Device(12, "OnePlus 7T", listOf("OnePlus7T"), true),
        Device(13, "OnePlus 7T (India)", listOf("OnePlus7T_IN"), true),
        Device(11, "OnePlus 7T Pro (EU)", listOf("OnePlus7TPro_EEA"), true),
        Device(12, "OnePlus 7T Pro", listOf("OnePlus7TPro"), true),
        Device(13, "OnePlus 7T Pro (India)", listOf("OnePlus7TPro_IN"), true)
    )

    private enum class TestDataType {
        BUILD_PROP_FILE,
        GETPROP_COMMAND_OUTPUT
    }

    companion object {
        private const val OsOtaVersionNumberLookupKey = "ro.build.version.ota"
        private const val OsVersionNumberLookupKey = "ro.rom.version, ro.oxygen.version, ro.build.ota.versionname"
        private const val SupportedBuildFingerprintKeys = "release-keys"
        private const val BuildFingerprintLookupKey = "ro.build.oemfingerprint, ro.build.fingerprint"
        private const val DeviceNameLookupKey = "ro.display.series, ro.build.product"
        private const val AbPartitionLayoutLookupKey = "ro.build.ab_update"
    }
}
