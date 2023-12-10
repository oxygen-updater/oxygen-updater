package com.oxygenupdater.utils

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import androidx.annotation.Dimension
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.DeviceOsSpec.Companion.CarrierExclusiveOxygenOs
import com.oxygenupdater.models.DeviceOsSpec.Companion.SupportedOxygenOs
import com.oxygenupdater.models.DeviceOsSpec.Companion.UnsupportedOs
import com.oxygenupdater.models.DeviceOsSpec.Companion.UnsupportedOxygenOs
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.device.defaultDeviceName
import java.time.ZoneId

object Utils {

    val ServerTimeZone: ZoneId = ZoneId.of("Europe/Amsterdam")

    private const val TAG = "Utils"
    private const val PlayServicesResolutionRequest = 9000

    fun dpToPx(context: Context, @Dimension(unit = Dimension.DP) dp: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics
    )

    /**
     * Checks if the Google Play Services are installed on the device.
     *
     * @return Returns if the Google Play Services are installed.
     */
    fun checkPlayServices(activity: Activity, showErrorIfMissing: Boolean): Boolean {
        logVerbose(TAG, "Executing Google Play Services check…")

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity)

        return if (resultCode != ConnectionResult.SUCCESS && showErrorIfMissing) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) googleApiAvailability.getErrorDialog(
                activity, resultCode, PlayServicesResolutionRequest
            )?.show()

            logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!")
            false
        } else {
            val result = resultCode == ConnectionResult.SUCCESS

            if (result) logVerbose(TAG, "Google Play Services are available.")
            else logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!")

            result
        }
    }

    fun checkDeviceOsSpec(devices: List<Device>?): DeviceOsSpec {
        // <brand>/<product>/<device>:<version.release>/<id>/<version.incremental>:<type>/<tags>
        val fingerprintParts = SystemVersionProperties.fingerprint.split("/").map {
            it.trim()
        }
        val firmwareIsSupported = fingerprintParts.size == 6
                && fingerprintParts[0].lowercase() == "oneplus"
                // must be `contains` and not a direct equality check
                && fingerprintParts[5].lowercase().contains("release-keys")

        if (devices.isNullOrEmpty()) {
            // To prevent incorrect results on empty server response.
            // This still checks if official ROM is used and if an OxygenOS version is found on the device.
            return if (firmwareIsSupported) SupportedOxygenOs else UnsupportedOxygenOs
        }

        return if (firmwareIsSupported) {
            // User's device is definitely running OxygenOS, now onto other checks…
            devices.forEach {
                // Find the user's device in the list of devices retrieved from the server
                if ((fingerprintParts.size > 2 && it.productNames.contains(fingerprintParts[1]))
                    || it.productNames.contains(SystemVersionProperties.oxygenDeviceName)
                ) return if (it.enabled) SupportedOxygenOs else {
                    // Device found, but is disabled, which means it's carrier-exclusive
                    // (only carrier-exclusive devices are disabled in the database)
                    CarrierExclusiveOxygenOs
                }
            }

            // Device not found among the server-provided list; assume it's a newly-released OnePlus device that we're yet to add support for
            UnsupportedOxygenOs
        } else {
            // Device isn't running OxygenOS at all. Note that it may still be a OnePlus device running a custom ROM.
            UnsupportedOs
        }
    }

    fun checkDeviceMismatch(devices: List<Device>?): Triple<Boolean, String, String> {
        val default = defaultDeviceName()
        val savedDeviceName = PrefManager.getString(PrefManager.KeyDevice, default) ?: default

        val productName = SystemVersionProperties.oxygenDeviceName
        val (deviceCode, regionCode) = productName.split("_", limit = 2).run {
            Pair(this[0], if (size > 1) this[1] else null)
        }

        val isStableTrack = SystemVersionProperties.osType.equals(
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

        val actualDeviceName = matchedDevice?.name ?: default

        // Don't show mismatch dialog if we don't know what the actual device is
        return Triple(if (actualDeviceName == default) false else isChosenDeviceIncorrect, savedDeviceName, actualDeviceName)
    }
}
