package com.oxygenupdater.models

import android.os.Build
import android.os.Build.UNKNOWN
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logVerbose
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.*

/**
 * Contains some properties of the OS / ROM installed on the device.
 *
 * Used to read / extract OnePlus-specific properties from the ROM.
 */
object SystemVersionProperties {

    private const val TAG = "SystemVersionProperties"
    private const val SECURITY_PATCH_LOOKUP_KEY = "ro.build.version.security_patch"

    /** Required for workaround #1 */
    private const val H2OS = "H2OS"

    /** Required for workaround #2 */
    private val OXYGENOS_PREFIX = "Oxygen ?OS ?".toRegex()

    /** Required for workaround #1 */
    private const val ROM_VERSION_LOOKUP_KEY = "ro.rom.version"

    /** Required for workaround #4 */
    private const val BUILD_SOFT_VERSION_LOOKUP_KEY = "ro.build.soft.version"

    /** Required for workaround #3 */
    private const val ONEPLUS_3 = "OnePlus3"

    /** Required for workaround #5 */
    private val ONEPLUS_7_SERIES = arrayOf("OnePlus7", "OnePlus7Pro")

    /** Required for workaround #4 & #5 */
    private val ONEPLUS_7T_SERIES = arrayOf("OnePlus7T", "OnePlus7TPro")

    /**
     * This isn't a workaround, but was introduced in the first Open Beta for 7T-series.
     * These keys will be checked on all devices, for better future-proofing, and saved to shared prefs.
     */
    private val BUILD_EU_LOOKUP_KEYS = arrayOf("ro.build.eu", "ro.vendor.build.eu")

    /**
     * This is a workaround for Nord 2 (maybe future devices too), since it doesn't have any of the above EU keys.
     * This key will be checked *after* the above keys are checked, as a backup (if above keys aren't found).
     *
     * Note: to keep things simple, we're checking if the value corresponding to this key starts with `EU`,
     * even though we've seen that (at least on Nord 2), it's `EUEX` for EU devices, and `IN` for India.
     */
    private const val VENDOR_OPLUS_REGIONMARK_LOOKUP_KEY = "ro.vendor.oplus.regionmark"

    /** Required for workaround #5*/
    private const val VENDOR_OP_INDIA = "ro.vendor.op.india"

    /** Required for [osType] */
    private const val BUILD_OS_TYPE_LOOKUP_KEY = "ro.build.os_type"

    /** Matchable name of the device */
    val oxygenDeviceName: String

    /** Human-readable OxygenOS version. Shown within the UI of the app */
    val oxygenOSVersion: String

    /** Used to check for updates and shown in the "Device" tab */
    val oxygenOSOTAVersion: String

    /** Shown in the "Device" tab */
    val securityPatchDate: String

    /** Used for checking device/OS compatibility */
    val fingerprint: String = Build.FINGERPRINT.trim()

    /**
     * This prop is present only on 7-series and above, on OS versions before the Oppo merger (ColorOS base).
     * Possible values:
     * 1. Stable: if property is present, but has no value
     * 2. Beta: if property is present and has "Beta" as its value
     * 3. Alpha: if property is present and has "Alpha" as its value
     * 4. <blank>: if property isn't present at all (i.e. unknown OS type)
     */
    val osType: String

    init {
        // Default to something sensible or set to Build.UNKNOWN
        var oxygenDeviceName = Build.PRODUCT
        var oxygenOSVersion = Build.DISPLAY
        var oxygenOSOTAVersion = UNKNOWN
        var osType = UNKNOWN
        var securityPatchDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else UNKNOWN // read later on

        try {
            if (!useSystemProperties.get()) throw UnsupportedOperationException("`useSystemProperties` is false")

            if (oxygenDeviceName == ONEPLUS_3) {
                // Workaround #3: don't use `ro.product.name` for OP3/3T; they have the same value.
                // Note: prior to v5.10.0, `ro.product.name` was read only on devices from 7-series onwards, and only
                // when `ro.display.series` also existed. It was meant as a workaround that added support for regional
                // variants. As a result, the app never used `ro.product.name` on devices & OS versions released after
                // the Oppo merger (ColorOS base), instead relying on `ro.build.product`. This caused issues with 10T
                // on OOS13, where `ro.build.product` had a value of `qssi` for some reason.
                oxygenDeviceName = pickFirstValid(BuildConfig.DEVICE_NAME_LOOKUP_KEYS, oxygenDeviceName) { _, value -> value }
            } else if (ONEPLUS_7T_SERIES.contains(oxygenDeviceName)) {
                // Workaround #4 (Build.PRODUCT + ro.build.soft.version): support Indian variants for 7T-series,
                // on OS versions released before the Oppo merger (ColorOS base).
                val buildSoftVersion = systemProperty(BUILD_SOFT_VERSION_LOOKUP_KEY)

                // Append _IN to mark device as Indian variant
                if (buildSoftVersion[0] == 'I') oxygenDeviceName += "_IN"
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && ONEPLUS_7_SERIES.contains(oxygenDeviceName)) {
                // Workaround #5 (Build.PRODUCT + ro.vendor.op.india): differentiate between 7-series GLO/IND on the
                // last two OOS11 builds (11.0.8.1 & 11.0.9.1). This property was used by system OTA to deliver the
                // correct regional OOS12 build (GLO: H.31, IND: H.30). There's another OOS12 workaround below.
                val india = systemProperty(VENDOR_OP_INDIA)
                if (india == "1" || india == "true") oxygenDeviceName += "_IN"
            }

            oxygenOSVersion = pickFirstValid(BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEYS, oxygenOSVersion) { key, value ->
                if (key == ROM_VERSION_LOOKUP_KEY) {
                    // Workaround #1 (ro.rom.version): ignore if value has the "H2OS" prefix (seen on OOS 2 & 3).
                    if (value.contains(H2OS)) return@pickFirstValid null

                    // Workaround #2 (ro.rom.version): remove redundant "Oxygen OS " prefix from value, because the app
                    // shows only the number or adds custom formatting. Seen on devices from OnePlus 7-series onwards,
                    // on OS versions released before the Oppo merger (ColorOS base).
                    if (value.contains(OXYGENOS_PREFIX)) {
                        value.replace(OXYGENOS_PREFIX, "")
                    } else value
                } else value
            }

            val euBooleanStr = pickFirstValid(BUILD_EU_LOOKUP_KEYS) { _, value -> value }
            PrefManager.putBoolean(PrefManager.PROPERTY_IS_EU_BUILD, if (euBooleanStr == UNKNOWN) {
                val pipeline = systemProperty(VENDOR_OPLUS_REGIONMARK_LOOKUP_KEY)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ONEPLUS_7_SERIES.contains(oxygenDeviceName) || ONEPLUS_7T_SERIES.contains(oxygenDeviceName)) {
                    // Workaround #5 (Build.PRODUCT + ro.vendor.oplus.regionmark): differentiate between GLO/IND
                    // builds for 7- & 7T-series on OOS12. This affects H.31/H.30 & F.17 builds, where the same
                    // model number is used for both regions. Not sure if future builds would also be affected.
                    if (pipeline.startsWith("IN")) oxygenDeviceName += "_IN"
                }
                pipeline.startsWith("EU")
            } else euBooleanStr.toBoolean())

            oxygenOSOTAVersion = systemProperty(BuildConfig.OS_OTA_VERSION_NUMBER_LOOKUP_KEY)

            // This prop is present only on 7-series and above
            osType = systemProperty(BUILD_OS_TYPE_LOOKUP_KEY).let {
                if (it.isBlank()) "Stable" else if (it == UNKNOWN) "" else it
            }

            // On Android >= 6/Marshmallow, security patch is picked up from `Build.VERSION.SECURITY_PATCH` above
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                securityPatchDate = systemProperty(SECURITY_PATCH_LOOKUP_KEY)
            }
        } catch (e: Exception) {
            logError(TAG, e.localizedMessage ?: "$e", e)

            try {
                logVerbose(TAG, "Fast path via `android.os.SystemProperties` failed; falling back to slower getprop output parse")

                val getBuildPropProcess = Runtime.getRuntime().exec("getprop")

                val scanner = Scanner(getBuildPropProcess.inputStream).useDelimiter("\\A")
                val properties = if (scanner.hasNext()) scanner.next() else ""

                getBuildPropProcess.destroy()

                if (oxygenDeviceName == ONEPLUS_3) {
                    // Workaround #3: don't use `ro.product.name` for OP3/3T; they have the same value.
                    // Note: prior to v5.10.0, `ro.product.name` was read only on devices from 7-series onwards, and only
                    // when `ro.display.series` also existed. It was meant as a workaround that added support for regional
                    // variants. As a result, the app never used `ro.product.name` on devices & OS versions released after
                    // the Oppo merger (ColorOS base), instead relying on `ro.build.product`. This caused issues with 10T
                    // on OOS13, where `ro.build.product` had a value of `qssi` for some reason.
                    oxygenDeviceName = readBuildPropItem(BuildConfig.DEVICE_NAME_LOOKUP_KEYS, properties, oxygenDeviceName)
                } else if (ONEPLUS_7T_SERIES.contains(oxygenDeviceName)) {
                    // Workaround #4 (Build.PRODUCT + ro.build.soft.version): support Indian variants for 7T-series,
                    // on OS versions released before the Oppo merger (ColorOS base).
                    val buildSoftVersion = readBuildPropItem(BUILD_SOFT_VERSION_LOOKUP_KEY, properties)

                    // Append _IN to mark device as Indian variant
                    if (buildSoftVersion[0] == 'I') oxygenDeviceName += "_IN"
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && ONEPLUS_7_SERIES.contains(oxygenDeviceName)) {
                    // Workaround #5 (Build.PRODUCT + ro.vendor.op.india): differentiate between 7-series GLO/IND on the
                    // last two OOS11 builds (11.0.8.1 & 11.0.9.1). This property was used by system OTA to deliver the
                    // correct regional OOS12 build (GLO: H.31, IND: H.30). There's another OOS12 workaround below.
                    val india = readBuildPropItem(VENDOR_OP_INDIA, properties)
                    if (india == "1" || india == "true") oxygenDeviceName += "_IN"
                }

                oxygenOSVersion = readBuildPropItem(BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEYS, properties, oxygenOSVersion)

                val euBooleanStr = readBuildPropItem(BUILD_EU_LOOKUP_KEYS, properties)
                PrefManager.putBoolean(PrefManager.PROPERTY_IS_EU_BUILD, if (euBooleanStr == UNKNOWN) {
                    val pipeline = readBuildPropItem(VENDOR_OPLUS_REGIONMARK_LOOKUP_KEY, properties)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ONEPLUS_7_SERIES.contains(oxygenDeviceName) || ONEPLUS_7T_SERIES.contains(oxygenDeviceName)) {
                        // Workaround #5 (Build.PRODUCT + ro.vendor.oplus.regionmark): differentiate between GLO/IND
                        // builds for 7- & 7T-series on OOS12. This affects H.31/H.30 & F.17 builds, where the same
                        // model number is used for both regions. Not sure if future builds would also be affected.
                        if (pipeline.startsWith("IN")) oxygenDeviceName += "_IN"
                    }
                    pipeline.startsWith("EU")
                } else euBooleanStr.toBoolean())

                oxygenOSOTAVersion = readBuildPropItem(BuildConfig.OS_OTA_VERSION_NUMBER_LOOKUP_KEY, properties)

                // This prop is present only on 7-series and above
                osType = readBuildPropItem(BUILD_OS_TYPE_LOOKUP_KEY, properties).let {
                    if (it.isBlank()) "Stable" else if (it == UNKNOWN) "" else it
                }

                // On Android >= 6/Marshmallow, security patch is picked up from `Build.VERSION.SECURITY_PATCH` above
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    securityPatchDate = readBuildPropItem(SECURITY_PATCH_LOOKUP_KEY, properties)
                }
            } catch (e: Exception) {
                logError(TAG, e.localizedMessage ?: "$e", e)
            }
        }

        this.oxygenDeviceName = oxygenDeviceName
        this.oxygenOSVersion = oxygenOSVersion
        this.oxygenOSOTAVersion = oxygenOSOTAVersion
        this.securityPatchDate = securityPatchDate
        this.osType = osType
    }

    private inline fun pickFirstValid(
        keys: Array<String>,
        default: String = UNKNOWN,
        crossinline workarounds: (String, String) -> String?,
    ): String {
        for ((key, value) in systemPropertyPairs(keys)) {
            if (value.isEmpty() || value == UNKNOWN) continue // invalid, skip to next iteration

            val newValue = workarounds(key, value)
            if (newValue != null) return newValue // break on first valid value

            // otherwise let the loop continue to the next iteration
        }

        return default
    }

    @Throws(IOException::class)
    private fun readBuildPropItem(
        itemKey: String,
        properties: String?,
    ) = readBuildPropItem(arrayOf(itemKey), properties)

    @Throws(IOException::class)
    private fun readBuildPropItem(
        itemKeys: Array<String>,
        properties: String?,
        default: String = UNKNOWN,
    ): String {
        if (properties.isNullOrEmpty()) return default

        var result = default

        // Some keys are not present on all devices, so check multiple in-order
        itemKeys.forEach { item ->
            val reader = BufferedReader(StringReader(properties))
            var inputLine: String?

            while (reader.readLine().also { inputLine = it } != null) {
                if (inputLine!!.contains(item)) {
                    // getprop output format is `[<item>]: [<value>]`, and we only need <value>.
                    // This is more efficient way to get rid of unneeded parts of the string, as
                    // opposed to `replace("[$item]: ", "").replace("[", "").replace("]", "")`.
                    result = inputLine!!.drop(
                        item.length + 5 /* 2 for surrounding `[]`, 2 for `: `, and 1 more for `[` */
                    ).dropLast(1 /* last `]` */)

                    if (item == ROM_VERSION_LOOKUP_KEY) {
                        // Workaround #1 (ro.rom.version): ignore if value has the "H2OS" prefix (seen on OOS 2 & 3).
                        if (result.contains(H2OS)) {
                            result = UNKNOWN
                            continue
                        }

                        // Workaround #2 (ro.rom.version): remove redundant "Oxygen OS " prefix from value, because the app
                        // shows only the number or adds custom formatting. Seen on devices from OnePlus 7-series onwards,
                        // on OS versions released before the Oppo merger (ColorOS base).
                        if (result.contains(OXYGENOS_PREFIX)) {
                            result = result.replace(OXYGENOS_PREFIX, "")
                        }
                    }

                    // Return the first successfully detected item. This because some keys have multiple values which all exist in the same properties file.
                    return result
                }
            }
        }

        return result
    }
}
