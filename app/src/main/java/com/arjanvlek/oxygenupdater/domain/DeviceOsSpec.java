package com.arjanvlek.oxygenupdater.domain;

import java.util.List;

/**
 * Used primarily in {@link com.arjanvlek.oxygenupdater.internal.Utils#checkDeviceOsSpec(SystemVersionProperties, List)}
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
public enum DeviceOsSpec {
	// for devices that are supported by the app
	SUPPORTED_OXYGEN_OS,
	// for carrier-exclusive devices like OnePlus 7T Pro 5G McLaren (T-Mobile)
	// these devices aren't supported by the app either, due to the "Local Upgrade" option being missin
	CARRIER_EXCLUSIVE_OXYGEN_OS,
	// for new devices that haven't been added to the app yet
	UNSUPPORTED_OXYGEN_OS,
	// for devices that aren't running OxygenOS
	// this may include OnePlus devices with a custom ROM installed
	UNSUPPORTED_OS;

	public boolean isDeviceOsSpecSupported() {
		return this == SUPPORTED_OXYGEN_OS;
	}
}
