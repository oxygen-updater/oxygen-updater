package com.arjanvlek.oxygenupdater.domain

import android.os.Build
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.NO_OXYGEN_OS
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.*

/**
 * Contains some properties of the OS / ROM installed on the device.
 *
 *
 * Used to read / extract  OnePlus-specific properties from the ROM.
 */
class SystemVersionProperties {
    /**
     * Matchable name of the device. Must be present in the Devices returned by ServerConnector
     */
    val oxygenDeviceName: String
    /**
     * Human-readable OxygenOS version. Shown within the UI of the app
     */
    val oxygenOSVersion: String
    /**
     * Technical / OTA version of OxygenOS. Used to check for updates and shown in Device Info tab
     */
    val oxygenOSOTAVersion: String
    /**
     * Security patch date. Must be looked up manually on Android versions < 6.0
     */
    val securityPatchDate: String
    /**
     * Fingerprint of the build. Used to check if the device uses an official build of OxygenOS
     */
    val oemFingerprint: String
    /**
     * Whether or not the device has an A/B partition layout. Required to generate a proper install
     * script for Automatic Update Installations (root feature)
     */
    val isABPartitionLayout: Boolean

    constructor() {
        var oxygenOSVersion = NO_OXYGEN_OS
        var oxygenOSOTAVersion = NO_OXYGEN_OS
        var oxygenDeviceName = NO_OXYGEN_OS
        var oemFingerprint = NO_OXYGEN_OS
        var securityPatchDate = NO_OXYGEN_OS
        var ABPartitionLayout = false

        try {
            val getBuildPropProcess = Runtime.getRuntime().exec("getprop")

            logVerbose(TAG, "Started fetching device properties using 'getprop' command...")

            val scanner = Scanner(getBuildPropProcess.inputStream).useDelimiter("\\A")
            val properties = if (scanner.hasNext()) scanner.next() else ""

            getBuildPropProcess.destroy()

            oxygenDeviceName = readBuildPropItem(BuildConfig.DEVICE_NAME_LOOKUP_KEY, properties, "Detected device: %s ...")
            oxygenOSVersion = readBuildPropItem(BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEY, properties, "Detected Oxygen OS ROM with version: %s ...")
            oxygenOSOTAVersion = readBuildPropItem(BuildConfig.OS_OTA_VERSION_NUMBER_LOOKUP_KEY, properties, "Detected Oxygen OS ROM with OTA version: %s ...")
            oemFingerprint = readBuildPropItem(BuildConfig.BUILD_FINGERPRINT_LOOKUP_KEY, properties, "Detected build fingerprint: %s ...")
            ABPartitionLayout = java.lang.Boolean.parseBoolean(readBuildPropItem(BuildConfig.AB_UPDATE_LOOKUP_KEY, properties, "Device has A/B partition layout: %s ..."))

            securityPatchDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH // Already available using Android API since Android 6.0
            } else {
                readBuildPropItem(SECURITY_PATCH_LOOKUP_KEY, properties, "Detected security patch level: %s ...")
            }

            logVerbose(TAG, "Finished fetching device properties using 'getprop' command...")

        } catch (e: Exception) {
            logError(TAG, e.localizedMessage, e)
        }

        this.oxygenDeviceName = oxygenDeviceName
        this.oxygenOSVersion = oxygenOSVersion
        this.oxygenOSOTAVersion = oxygenOSOTAVersion
        this.oemFingerprint = oemFingerprint
        this.securityPatchDate = securityPatchDate
        this.isABPartitionLayout = ABPartitionLayout
    }

    // Only called from within the tests, as we do not want to call the real 'getprop' command from there.
    constructor(oxygenDeviceName: String, oxygenOSVersion: String, oxygenOSOTAVersion: String, securityPatchDate: String, oemFingerprint: String, ABPartitionLayout: Boolean) {
        println("Warning: SystemVersionProperties was constructed using a debug constructor. This should only happen during unit tests!")
        this.oxygenDeviceName = oxygenDeviceName
        this.oxygenOSVersion = oxygenOSVersion
        this.oxygenOSOTAVersion = oxygenOSOTAVersion
        this.securityPatchDate = securityPatchDate
        this.oemFingerprint = oemFingerprint
        this.isABPartitionLayout = ABPartitionLayout
    }

    @Throws(IOException::class)
    private fun readBuildPropItem(itemKeys: String, buildProperties: String?, logText: String?): String {
        if (buildProperties == null || buildProperties.isEmpty()) {
            return NO_OXYGEN_OS
        }

        var result = NO_OXYGEN_OS

        // Some keys are not present on all devices. Therefore, we'll need support for multiple keys in a single string.
        // If the first key is not present on this device, try the next key. We split the key string by ", "
        val items = itemKeys
                .trim { it <= ' ' }
                .replace(" ", "")
                .split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (item in items) {
            do {
                val inputLine = BufferedReader(StringReader(buildProperties)).readLine()
                if (inputLine.contains(item)) {
                    // Remove brackets ([ and ]) and ":" from the getprop command output line
                    result = inputLine.replace("[$item]: ", "")
                    result = result.replace("[", "")
                    result = result.replace("]", "")

                    // @hack #1 (OS_VERSION_NUMBER_LOOKUP_KEY): OxygenOS 2.0.0 - 3.x sometimes contain incorrect H2OS values for ro.rom.version
                    // if this is the case, discard the value and try with the next item in "items" array
                    if (item == RO_ROM_VERSION_LOOKUP_KEY && result.contains(RO_ROM_VERSION_H2OS)) {
                        result = NO_OXYGEN_OS
                        continue
                    }

                    // @hack #2 (OS_VERSION_NUMBER_LOOKUP_KEY): OnePlus 7 and later store hardcoded "Oxygen OS " in their version number of the firmware.
                    // As the app only shows the number or ads custom formatting, remove this prefix
                    if (item == RO_ROM_VERSION_LOOKUP_KEY && result.contains(RO_ROM_VERSION_OXYGENOS_PREFIX)) {
                        result = result.replace(RO_ROM_VERSION_OXYGENOS_PREFIX, "")
                    }

                    // @hack #3 (DEVICE_NAME_LOOKUP_KEY / support for regional device variants): OnePlus 7 Pro and newer devices come in regional variants which cannot be detected by ro.display.series.
                    // However, the property that *does* contain it (ro.product.name) does not play nice with values present on older devices.
                    // Bypass: if the key is 'ro.display.series' and the value is one of these devices, then read 'ro.product.name' instead to detect the correct device
                    if (item == RO_DISPLAY_SERIES_LOOKUP_KEY && RO_PRODUCT_NAME_LOOKUP_DEVICES.contains(result)) {
                        // Android Logger class is not loaded during unit tests, so omit logging if called from test.
                        val logMessage = if (logText != null) "Detected $result variant: %s" else null
                        result = readBuildPropItem(RO_PRODUCT_NAME_LOOKUP_KEY, buildProperties, logMessage)
                    }

                    if (logText != null) {
                        logVerbose(TAG, String.format(logText, result))
                    }

                    return result // Return the first successfully detected item. This because some keys have multiple values which all exist in the same properties file.
                }
            } while (inputLine != null)
        }
        return result
    }

    companion object {
        private const val TAG = "SystemVersionProperties"
        private const val SECURITY_PATCH_LOOKUP_KEY = "ro.build.version.security_patch"
        private const val RO_ROM_VERSION_LOOKUP_KEY = "ro.rom.version"
        // @hack #1 (OS_VERSION_NUMBER_LOOKUP_KEY): OxygenOS 2.0.0 - 3.x sometimes contain an incorrect "H2OS 1.x" value for ro.rom.version
        // if this is the case, discard the value and try with the next item in "items" array
        private const val RO_ROM_VERSION_H2OS = "H2OS" // @GitHub contributors, this should never have to change, as it only applies to very old devices.
        // @hack #2 (OS_VERSION_NUMBER_LOOKUP_KEY): OnePlus 7 and later store hardcoded "Oxygen OS " in their version number of the firmware.
        // As the app only shows the number or ads custom formatting, remove this prefix
        private const val RO_ROM_VERSION_OXYGENOS_PREFIX = "Oxygen OS " // @GitHub contributors, change this value if ro.oxygen.version contains another prefix than "Oxygen OS ".
        // @hack #3 (DEVICE_NAME_LOOKUP_KEY / OnePlus 7 Pro Support): OnePlus 7 Pro and newer come in regional variants which cannot be detected by ro.display.series.
        // However, its alternative (ro.product.name) does not play nice with values present on older devices.
        // Bypass: if the key is 'ro.display.series' and the value is one of the devices listed below, then read 'ro.product.name' instead to detect the correct device
        private const val RO_DISPLAY_SERIES_LOOKUP_KEY = "ro.display.series"
        private const val RO_PRODUCT_NAME_LOOKUP_KEY = "ro.product.name"
        // @GitHub contributors, add ro.display.series values of new OP devices *HERE*
        private val RO_PRODUCT_NAME_LOOKUP_DEVICES = listOf("OnePlus 7", "OnePlus 7 Pro", "OnePlus 7 Pro 5G")
    }
}