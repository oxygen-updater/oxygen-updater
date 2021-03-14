package com.oxygenupdater.utils

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import androidx.annotation.Dimension
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.OxygenUpdater.Companion.isNetworkAvailable
import com.oxygenupdater.R
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS
import com.oxygenupdater.models.DeviceOsSpec.SUPPORTED_OXYGEN_OS
import com.oxygenupdater.models.DeviceOsSpec.UNSUPPORTED_OS
import com.oxygenupdater.models.DeviceOsSpec.UNSUPPORTED_OXYGEN_OS
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.Logger.logVerbose
import org.koin.java.KoinJavaComponent.inject
import org.threeten.bp.ZoneId
import kotlin.system.exitProcess

@Suppress("unused")
object Utils {

    val SERVER_TIME_ZONE: ZoneId = ZoneId.of("Europe/Amsterdam")

    private const val TAG = "Utils"
    private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

    private val systemVersionProperties by inject(SystemVersionProperties::class.java)

    /**
     * Originally part of [com.google.android.gms.common.util.NumberUtils], removed in later versions
     *
     * @param string the string to test
     *
     * @return true if string is a number, false otherwise
     */
    fun isNumeric(string: String) = try {
        string.toLong()
        true
    } catch (e: NumberFormatException) {
        false
    }

    fun dpToPx(context: Context, @Dimension(unit = Dimension.DP) dp: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics
    )

    fun spToPx(context: Context, @Dimension(unit = Dimension.SP) sp: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        context.resources.displayMetrics
    )

    /**
     * Checks if the Google Play Services are installed on the device.
     *
     * @return Returns if the Google Play Services are installed.
     */
    fun checkPlayServices(activity: Activity, showErrorIfMissing: Boolean): Boolean {
        logVerbose(TAG, "Executing Google Play Services check...")

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity)

        return if (resultCode != ConnectionResult.SUCCESS && showErrorIfMissing) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(
                    activity,
                    resultCode,
                    PLAY_SERVICES_RESOLUTION_REQUEST
                )?.show()
            } else {
                exitProcess(0)
            }

            logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!")
            false
        } else {
            val result = resultCode == ConnectionResult.SUCCESS

            if (result) {
                logVerbose(TAG, "Google Play Services are available.")
            } else {
                logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!")
            }

            result
        }
    }

    fun checkNetworkConnection() = isNetworkAvailable.value == true

    fun checkDeviceOsSpec(devices: List<Device>?): DeviceOsSpec {
        val oemFingerPrint: String? = systemVersionProperties.oemFingerprint
        val oxygenOsVersion: String? = systemVersionProperties.oxygenOSVersion

        val firmwareIsSupported = !oemFingerPrint.isNullOrEmpty()
                && oemFingerPrint != OxygenUpdater.NO_OXYGEN_OS
                && oemFingerPrint.contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS)
                && !oxygenOsVersion.isNullOrEmpty()
                && oxygenOsVersion != OxygenUpdater.NO_OXYGEN_OS

        if (devices.isNullOrEmpty()) {
            // To prevent incorrect results on empty server response. This still checks if official ROM is used and if an OxygenOS version is found on the device.
            return if (firmwareIsSupported) {
                SUPPORTED_OXYGEN_OS
            } else {
                UNSUPPORTED_OS
            }
        }

        return if (firmwareIsSupported) {
            // user's device is definitely running OxygenOS, now onto other checks...
            devices.forEach {
                // find the user's device in the list of devices retrieved from the server
                if (it.productNames.contains(systemVersionProperties.oxygenDeviceName)) {
                    return if (it.enabled) {
                        // device found, and is enabled, which means it is supported
                        SUPPORTED_OXYGEN_OS
                    } else {
                        // device found, but is disabled, which means it's a carrier-exclusive
                        // because only carrier-exclusive devices are disabled in the database
                        CARRIER_EXCLUSIVE_OXYGEN_OS
                    }
                }
            }

            // device not found among the server-provided list
            // hence, must be a newly-released OnePlus device that we're yet to add support for
            UNSUPPORTED_OXYGEN_OS
        } else {
            // device isn't running OxygenOS at all
            // note: the device may very well be a OnePlus device, but in this case it's running a custom ROM, which we don't support duh
            UNSUPPORTED_OS
        }
    }

    fun checkDeviceMismatch(context: Context, devices: List<Device>?): Triple<Boolean, String, String> {
        var actualDeviceName = context.getString(R.string.device_information_unknown)
        val savedDeviceName = SettingsManager.getPreference(
            SettingsManager.PROPERTY_DEVICE,
            actualDeviceName
        )

        val productName = systemVersionProperties.oxygenDeviceName
        val (deviceCode, regionCode) = productName.split("_", limit = 2).run {
            Pair(this[0], if (size > 1) this[1] else null)
        }

        val isStableTrack = systemVersionProperties.osType.equals(
            "stable",
            true
        )

        var chosenDevice: Device? = null
        var matchedDevice: Device? = null
        devices?.forEach {
            if (it.name == savedDeviceName) {
                chosenDevice = it
            }

            // Filter out disabled devices, because that's handled
            // by [checkDeviceOsSpec] already.
            if (it.enabled && it.productNames.contains(productName)) {
                matchedDevice = it
            }

            // Break out if we found what we're looking for
            if (chosenDevice != null && matchedDevice != null) {
                return@forEach
            }
        }

        // If the currently installed OS track is "Stable", `productName` should match exactly.
        // Otherwise we should check `deviceCode` and `regionCode` individually, because other
        // tracks (Alpha, Beta, Developer Preview, etc.) may not have any regional builds.
        // In such cases, `regionCode` defaults to global (i.e. `null` instead of EEA/IND).
        // So we must check if `deviceCode` and `regionCode` match (or if `regionCode` is `null`)
        // Otherwise we assume the user is responsible enough to choose the correct device
        // according to their region.
        val isChosenDeviceIncorrect = chosenDevice?.productNames?.all {
            if (isStableTrack) {
                productName != it
            } else {
                it.split("_", limit = 2).run {
                    val chosenDeviceCode = this[0]
                    val chosenRegionCode = if (size > 1) this[1] else null
                    deviceCode != chosenDeviceCode || (regionCode != null && regionCode != chosenRegionCode)
                }
            }
        } ?: false

        actualDeviceName = matchedDevice?.name ?: actualDeviceName

        return Triple(isChosenDeviceIncorrect, savedDeviceName, actualDeviceName)
    }
}
