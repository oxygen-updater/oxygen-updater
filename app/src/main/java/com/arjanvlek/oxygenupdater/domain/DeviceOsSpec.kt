package com.arjanvlek.oxygenupdater.domain

/**
 * Used primarily in [com.arjanvlek.oxygenupdater.internal.Utils.checkDeviceOsSpec]
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
@Suppress("unused")
enum class DeviceOsSpec {
    // for devices that are supported by the app
    SUPPORTED_OXYGEN_OS,
    // for carrier-exclusive devices like OnePlus 7T Pro 5G McLaren (T-Mobile)
    // these devices aren't supported by the app either, due to the "Local Upgrade" option being missing
    CARRIER_EXCLUSIVE_OXYGEN_OS,
    // for new devices that haven't been added to the app yet
    UNSUPPORTED_OXYGEN_OS,
    // for devices that aren't running OxygenOS
    // this may include OnePlus devices with a custom ROM installed
    UNSUPPORTED_OS;

    val isDeviceOsSpecSupported: Boolean
        get() = this == SUPPORTED_OXYGEN_OS
}
