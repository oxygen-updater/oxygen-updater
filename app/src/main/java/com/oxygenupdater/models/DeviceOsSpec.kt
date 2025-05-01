package com.oxygenupdater.models

import androidx.compose.runtime.Immutable

/**
 * Used primarily in [com.oxygenupdater.utils.Utils.checkDeviceOsSpec]
 */
@Immutable
@JvmInline
value class DeviceOsSpec private constructor(val value: Int) {

    override fun toString() = "DeviceOsSpec." + when (this) {
        SupportedDeviceAndOs -> "SupportedDeviceAndOs"
        CarrierExclusiveOxygenOs -> "CarrierExclusiveOxygenOs"
        UnsupportedDevice -> "UnsupportedDevice"
        UnsupportedDeviceAndOs -> "UnsupportedDeviceAndOs"
        else -> "Invalid"
    }

    companion object {
        /** For devices that are supported by the app */
        val SupportedDeviceAndOs = DeviceOsSpec(0)

        /**
         * For carrier-exclusive devices like OnePlus 7T Pro 5G McLaren (T-Mobile).
         * These devices aren't supported by the app either, due to the "Local Upgrade" option being missing.
         */
        val CarrierExclusiveOxygenOs = DeviceOsSpec(1)

        /** For new devices that haven't been added to the app yet */
        val UnsupportedDevice = DeviceOsSpec(2)

        /**
         * For devices that aren't running OxygenOS/ColorOS.
         * This may include OPPO/OnePlus devices with a custom ROM installed.
         */
        val UnsupportedDeviceAndOs = DeviceOsSpec(3)
    }
}
