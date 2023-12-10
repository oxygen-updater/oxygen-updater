package com.oxygenupdater.models

import android.os.Build
import android.os.Build.UNKNOWN
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.utils.logError
import com.oxygenupdater.utils.logVerbose
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.Scanner

/**
 * Contains some properties of the OS / ROM installed on the device.
 *
 * Used to read / extract OnePlus-specific properties from the ROM.
 */
object SystemVersionProperties {

    private const val TAG = "SystemVersionProperties"
    private const val SecurityPatchLookupKey = "ro.build.version.security_patch"

    /** Required for workaround #1 */
    private const val H2OS = "H2OS"

    /** Required for workaround #2 */
    private val OxygenOsPrefix = "Oxygen ?OS ?".toRegex()

    /** Required for workaround #1 */
    private const val RomVersionLookupKey = "ro.rom.version"

    /** Required for workaround #4 */
    private const val BuildSoftVersionLookupKey = "ro.build.soft.version"

    /** Required for workaround #3 */
    private const val OnePlus3 = "OnePlus3"

    /** Required for workaround #6 */
    private const val OnePlusPad = "OPD2203"

    /** Required for workaround #5 */
    private val OnePlus7Series = arrayOf("OnePlus7", "OnePlus7Pro")

    /** Required for workaround #4 & #5 */
    private val OnePlus7TSeries = arrayOf("OnePlus7T", "OnePlus7TPro")

    /**
     * This isn't a workaround, but was introduced in the first Open Beta for 7T-series.
     * These keys will be checked on all devices, for better future-proofing, and saved to shared prefs.
     */
    private val BuildEuLookupKeys = arrayOf("ro.build.eu", "ro.vendor.build.eu")

    /**
     * This is a workaround for Nord 2 (maybe future devices too), since it doesn't have any of the above EU keys.
     * This key will be checked *after* the above keys are checked, as a backup (if above keys aren't found).
     *
     * Note: to keep things simple, we're checking if the value corresponding to this key starts with `EU`,
     * even though we've seen that (at least on Nord 2), it's `EUEX` for EU devices, and `IN` for India.
     */
    private const val VendorOplusRegionMarkLookupKey = "ro.vendor.oplus.regionmark"

    /** Required for workaround #5*/
    private const val VendorOpIndia = "ro.vendor.op.india"

    /** Required for [osType] */
    private const val BuildOsTypeLookupKey = "ro.build.os_type"

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
            if (!useSystemProperties) throw UnsupportedOperationException("`useSystemProperties` is false")

            if (oxygenDeviceName == OnePlus3) {
                // Workaround #3: don't use `ro.product.name` for OP3/3T; they have the same value.
                // Note: prior to v5.10.0, `ro.product.name` was read only on devices from 7-series onwards, and only
                // when `ro.display.series` also existed. It was meant as a workaround that added support for regional
                // variants. As a result, the app never used `ro.product.name` on devices & OS versions released after
                // the Oppo merger (ColorOS base), instead relying on `ro.build.product`. This caused issues with 10T
                // on OOS13, where `ro.build.product` had a value of `qssi` for some reason.
                oxygenDeviceName = pickFirstValid(BuildConfig.DEVICE_NAME_LOOKUP_KEYS, oxygenDeviceName) { _, value -> value }
            } else if (OnePlus7TSeries.contains(oxygenDeviceName)) {
                // Workaround #4 (Build.PRODUCT + ro.build.soft.version): support Indian variants for 7T-series,
                // on OS versions released before the Oppo merger (ColorOS base).
                val buildSoftVersion = systemProperty(BuildSoftVersionLookupKey)

                // Append _IN to mark device as Indian variant
                if (buildSoftVersion.getOrNull(0) == 'I') oxygenDeviceName += "_IN"
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && OnePlus7Series.contains(oxygenDeviceName)) {
                // Workaround #5 (Build.PRODUCT + ro.vendor.op.india): differentiate between 7-series GLO/IND on the
                // last two OOS11 builds (11.0.8.1 & 11.0.9.1). This property was used by system OTA to deliver the
                // correct regional OOS12 build (GLO: H.31, IND: H.30). There's another OOS12 workaround below.
                val india = systemProperty(VendorOpIndia)
                if (india == "1" || india == "true") oxygenDeviceName += "_IN"
            }

            // Prefer `Build.DISPLAY` on Android>13/T, to pick the new OOS13.1 format: KB2001_13.1.0.513(EX01),
            // which corresponds to the KB2001_11_F.66 version number. Below OOS13.1, `Build.DISPLAY` is the version
            // number, so we're not losing any info.
            if (oxygenOSVersion == UNKNOWN || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) oxygenOSVersion = pickFirstValid(
                BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEYS, oxygenOSVersion
            ) { key, value ->
                if (key != RomVersionLookupKey) return@pickFirstValid value

                // Workaround #1 (ro.rom.version): ignore if value has the "H2OS" prefix (seen on OOS 2 & 3).
                if (value.contains(H2OS)) return@pickFirstValid null

                // Workaround #2 (ro.rom.version): remove redundant "Oxygen OS " prefix from value, because the app
                // shows only the number or adds custom formatting. Seen on devices from OnePlus 7-series onwards,
                // on OS versions released before the Oppo merger (ColorOS base).
                if (value.contains(OxygenOsPrefix)) value.replace(OxygenOsPrefix, "") else value
            }

            val pipeline = systemProperty(VendorOplusRegionMarkLookupKey)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (oxygenDeviceName == OnePlusPad) {
                    // Skip EUEX because that's already supported as OPD2203EEA
                    if (pipeline != "EUEX") oxygenDeviceName += pipeline
                } else if (OnePlus7Series.contains(oxygenDeviceName) || OnePlus7TSeries.contains(oxygenDeviceName)) {
                    // Workaround #5 (Build.PRODUCT + ro.vendor.oplus.regionmark): differentiate between GLO/IND
                    // builds for 7- & 7T-series on OOS12. This affects H.31/H.30 & F.17 builds, where the same
                    // model number is used for both regions. Not sure if future builds would also be affected.
                    if (pipeline.startsWith("IN")) oxygenDeviceName += "_IN"
                }
            }

            val euBooleanStr = pickFirstValid(BuildEuLookupKeys) { _, value -> value }
            PrefManager.putBoolean(
                PrefManager.KeyIsEuBuild,
                if (euBooleanStr == UNKNOWN) pipeline.startsWith("EU") else euBooleanStr.toBoolean()
            )

            oxygenOSOTAVersion = systemProperty(BuildConfig.OS_OTA_VERSION_NUMBER_LOOKUP_KEY)

            // This prop is present only on 7-series and above
            osType = systemProperty(BuildOsTypeLookupKey).let {
                if (it.isBlank()) "Stable" else if (it == UNKNOWN) "" else it
            }

            // On Android >= 6/Marshmallow, security patch is picked up from `Build.VERSION.SECURITY_PATCH` above
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                securityPatchDate = systemProperty(SecurityPatchLookupKey)
            }
        } catch (e: Exception) {
            logError(TAG, e.localizedMessage ?: "$e", e)

            try {
                logVerbose(TAG, "Fast path via `android.os.SystemProperties` failed; falling back to slower getprop output parse")

                val getBuildPropProcess = Runtime.getRuntime().exec("getprop")

                val scanner = Scanner(getBuildPropProcess.inputStream).useDelimiter("\\A")
                val properties = if (scanner.hasNext()) scanner.next() else ""

                getBuildPropProcess.destroy()

                if (oxygenDeviceName == OnePlus3) {
                    // Workaround #3: don't use `ro.product.name` for OP3/3T; they have the same value.
                    // Note: prior to v5.10.0, `ro.product.name` was read only on devices from 7-series onwards, and only
                    // when `ro.display.series` also existed. It was meant as a workaround that added support for regional
                    // variants. As a result, the app never used `ro.product.name` on devices & OS versions released after
                    // the Oppo merger (ColorOS base), instead relying on `ro.build.product`. This caused issues with 10T
                    // on OOS13, where `ro.build.product` had a value of `qssi` for some reason.
                    oxygenDeviceName = readBuildPropItem(BuildConfig.DEVICE_NAME_LOOKUP_KEYS, properties, oxygenDeviceName)
                } else if (OnePlus7TSeries.contains(oxygenDeviceName)) {
                    // Workaround #4 (Build.PRODUCT + ro.build.soft.version): support Indian variants for 7T-series,
                    // on OS versions released before the Oppo merger (ColorOS base).
                    val buildSoftVersion = readBuildPropItem(BuildSoftVersionLookupKey, properties)

                    // Append _IN to mark device as Indian variant
                    if (buildSoftVersion.getOrNull(0) == 'I') oxygenDeviceName += "_IN"
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && OnePlus7Series.contains(oxygenDeviceName)) {
                    // Workaround #5 (Build.PRODUCT + ro.vendor.op.india): differentiate between 7-series GLO/IND on the
                    // last two OOS11 builds (11.0.8.1 & 11.0.9.1). This property was used by system OTA to deliver the
                    // correct regional OOS12 build (GLO: H.31, IND: H.30). There's another OOS12 workaround below.
                    val india = readBuildPropItem(VendorOpIndia, properties)
                    if (india == "1" || india == "true") oxygenDeviceName += "_IN"
                }

                // Prefer `Build.DISPLAY` on Android>13/T, to pick the new OOS13.1 format: KB2001_13.1.0.513(EX01),
                // which corresponds to the KB2001_11_F.66 version number. Below OOS13.1, `Build.DISPLAY` is the version
                // number, so we're not losing any info.
                if (oxygenOSVersion == UNKNOWN || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) oxygenOSVersion = readBuildPropItem(
                    BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEYS, properties, oxygenOSVersion
                )

                val euBooleanStr = readBuildPropItem(BuildEuLookupKeys, properties)
                PrefManager.putBoolean(
                    PrefManager.KeyIsEuBuild, if (euBooleanStr == UNKNOWN) {
                        val pipeline = readBuildPropItem(VendorOplusRegionMarkLookupKey, properties)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (oxygenDeviceName == OnePlusPad) {
                                // Skip EUEX because that's already supported as OPD2203EEA
                                if (pipeline != "EUEX") oxygenDeviceName += pipeline
                            } else if (OnePlus7Series.contains(oxygenDeviceName) || OnePlus7TSeries.contains(oxygenDeviceName)) {
                                // Workaround #5 (Build.PRODUCT + ro.vendor.oplus.regionmark): differentiate between GLO/IND
                                // builds for 7- & 7T-series on OOS12. This affects H.31/H.30 & F.17 builds, where the same
                                // model number is used for both regions. Not sure if future builds would also be affected.
                                if (pipeline.startsWith("IN")) oxygenDeviceName += "_IN"
                            }
                        }
                        pipeline.startsWith("EU")
                    } else euBooleanStr.toBoolean()
                )

                oxygenOSOTAVersion = readBuildPropItem(BuildConfig.OS_OTA_VERSION_NUMBER_LOOKUP_KEY, properties)

                // This prop is present only on 7-series and above
                osType = readBuildPropItem(BuildOsTypeLookupKey, properties).let {
                    if (it.isBlank()) "Stable" else if (it == UNKNOWN) "" else it
                }

                // On Android >= 6/Marshmallow, security patch is picked up from `Build.VERSION.SECURITY_PATCH` above
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    securityPatchDate = readBuildPropItem(SecurityPatchLookupKey, properties)
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
        workarounds: (key: String, value: String) -> String?,
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
    private fun readBuildPropItem(itemKey: String, properties: String?) = readBuildPropItem(arrayOf(itemKey), properties)

    @Throws(IOException::class)
    private fun readBuildPropItem(itemKeys: Array<String>, properties: String?, default: String = UNKNOWN): String {
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

                    if (item == RomVersionLookupKey) {
                        // Workaround #1 (ro.rom.version): ignore if value has the "H2OS" prefix (seen on OOS 2 & 3).
                        if (result.contains(H2OS)) {
                            result = UNKNOWN
                            continue
                        }

                        // Workaround #2 (ro.rom.version): remove redundant "Oxygen OS " prefix from value, because the app
                        // shows only the number or adds custom formatting. Seen on devices from OnePlus 7-series onwards,
                        // on OS versions released before the Oppo merger (ColorOS base).
                        if (result.contains(OxygenOsPrefix)) {
                            result = result.replace(OxygenOsPrefix, "")
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
