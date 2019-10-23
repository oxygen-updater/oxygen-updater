package com.arjanvlek.oxygenupdater.domain;


import android.os.Build;

import com.arjanvlek.oxygenupdater.BuildConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose;

/**
 * Contains some properties of the OS / ROM installed on the device.
 * <p>
 * Used to read / extract  OnePlus-specific properties from the ROM.
 */
public class SystemVersionProperties {

	private static final String TAG = "SystemVersionProperties";
	private static final String SECURITY_PATCH_LOOKUP_KEY = "ro.build.version.security_patch";
	private static final String RO_ROM_VERSION_LOOKUP_KEY = "ro.rom.version";
	// @hack #1 (OS_VERSION_NUMBER_LOOKUP_KEY): OxygenOS 2.0.0 - 3.x sometimes contain an incorrect "H2OS 1.x" value for ro.rom.version
	// if this is the case, discard the value and try with the next item in "items" array
	private static final String RO_ROM_VERSION_H2OS = "H2OS"; // @GitHub contributors, this should never have to change, as it only applies to very old devices.
	// @hack #2 (OS_VERSION_NUMBER_LOOKUP_KEY): OnePlus 7 and later store hardcoded "Oxygen OS " in their version number of the firmware.
	// As the app only shows the number or ads custom formatting, remove this prefix
	private static final String RO_ROM_VERSION_OXYGENOS_PREFIX = "Oxygen OS "; // @GitHub contributors, change this value if ro.oxygen.version contains another prefix than "Oxygen OS ".
	// @hack #3 (DEVICE_NAME_LOOKUP_KEY / OnePlus 7 Pro Support): OnePlus 7 Pro and newer come in regional variants which cannot be detected by ro.display.series.
	// However, its alternative (ro.product.name) does not play nice with values present on older devices.
	// Bypass: if the key is 'ro.display.series' and the value is one of the devices listed below, then read 'ro.product.name' instead to detect the correct device
	private static final String RO_DISPLAY_SERIES_LOOKUP_KEY = "ro.display.series";
	private static final String RO_PRODUCT_NAME_LOOKUP_KEY = "ro.product.name";
	private static final String RO_BUILD_SOFT_VERSION_LOOKUP_KEY = "ro.build.soft.version";
	// @GitHub contributors, add ro.display.series values of new OP devices *HERE*
	private static final List<String> RO_PRODUCT_NAME_LOOKUP_DEVICES = Arrays.asList(
			"OnePlus 7",
			"OnePlus 7 Pro",
			"OnePlus 7 Pro 5G",
			"OnePlus 7T",
			"OnePlus 7T Pro"
	);
	// @GitHub contributors, add ro.product.name values of new OP devices *HERE*
	// This is used only to distinguish between Indian and international variants
	// OnePlus started rolling out India-specific features with the 7T-series, but
	// there was no distinction between ro.product.name in Indian and international variants
	// Only workaround is to read ro.build.version.ota
	private static final List<String> RO_BUILD_SOFT_VERSION_LOOKUP_DEVICES = Arrays.asList(
			"OnePlus7T",
			"OnePlus7TPro"
	);
	/**
	 * Matchable name of the device. Must be present in the Devices returned by ServerConnector
	 */
	private final String oxygenDeviceName;
	/**
	 * Human-readable OxygenOS version. Shown within the UI of the app
	 */
	private final String oxygenOSVersion;
	/**
	 * Technical / OTA version of OxygenOS. Used to check for updates and shown in Device Info tab
	 */
	private final String oxygenOSOTAVersion;
	/**
	 * Security patch date. Must be looked up manually on Android versions < 6.0
	 */
	private final String securityPatchDate;
	/**
	 * Fingerprint of the build. Used to check if the device uses an official build of OxygenOS
	 */
	private final String oemFingerprint;
	/**
	 * Whether or not the device has an A/B partition layout. Required to generate a proper install
	 * script for Automatic Update Installations (root feature)
	 */
	private final boolean ABPartitionLayout;

	public SystemVersionProperties() {
		String oxygenOSVersion = NO_OXYGEN_OS;
		String oxygenOSOTAVersion = NO_OXYGEN_OS;
		String oxygenDeviceName = NO_OXYGEN_OS;
		String oemFingerprint = NO_OXYGEN_OS;
		String securityPatchDate = NO_OXYGEN_OS;
		boolean ABPartitionLayout = false;

		try {
			Process getBuildPropProcess = Runtime.getRuntime().exec("getprop");

			logVerbose(TAG, "Started fetching device properties using 'getprop' command...");

			Scanner scanner = new Scanner(getBuildPropProcess.getInputStream()).useDelimiter("\\A");
			String properties = scanner.hasNext() ? scanner.next() : "";

			getBuildPropProcess.destroy();

			oxygenDeviceName = readBuildPropItem(BuildConfig.DEVICE_NAME_LOOKUP_KEY, properties, "Detected device: %s ...");
			oxygenOSVersion = readBuildPropItem(BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEY, properties, "Detected Oxygen OS ROM with version: %s ...");
			oxygenOSOTAVersion = readBuildPropItem(BuildConfig.OS_OTA_VERSION_NUMBER_LOOKUP_KEY, properties, "Detected Oxygen OS ROM with OTA version: %s ...");
			oemFingerprint = readBuildPropItem(BuildConfig.BUILD_FINGERPRINT_LOOKUP_KEY, properties, "Detected build fingerprint: %s ...");
			ABPartitionLayout = Boolean.parseBoolean(readBuildPropItem(BuildConfig.AB_UPDATE_LOOKUP_KEY, properties, "Device has A/B partition layout: %s ..."));

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				securityPatchDate = Build.VERSION.SECURITY_PATCH; // Already available using Android API since Android 6.0
			} else {
				securityPatchDate = readBuildPropItem(SECURITY_PATCH_LOOKUP_KEY, properties, "Detected security patch level: %s ...");
			}

			logVerbose(TAG, "Finished fetching device properties using 'getprop' command...");

		} catch (Exception e) {
			logError(TAG, e.getLocalizedMessage(), e);
		}

		this.oxygenDeviceName = oxygenDeviceName;
		this.oxygenOSVersion = oxygenOSVersion;
		this.oxygenOSOTAVersion = oxygenOSOTAVersion;
		this.oemFingerprint = oemFingerprint;
		this.securityPatchDate = securityPatchDate;
		this.ABPartitionLayout = ABPartitionLayout;
	}

	// Only called from within the tests, as we do not want to call the real 'getprop' command from there.
	public SystemVersionProperties(String oxygenDeviceName, String oxygenOSVersion, String oxygenOSOTAVersion, String securityPatchDate, String oemFingerprint, boolean ABPartitionLayout) {
		System.out.println("Warning: SystemVersionProperties was constructed using a debug constructor. This should only happen during unit tests!");
		this.oxygenDeviceName = oxygenDeviceName;
		this.oxygenOSVersion = oxygenOSVersion;
		this.oxygenOSOTAVersion = oxygenOSOTAVersion;
		this.securityPatchDate = securityPatchDate;
		this.oemFingerprint = oemFingerprint;
		this.ABPartitionLayout = ABPartitionLayout;
	}

	private String readBuildPropItem(String itemKeys, String buildProperties, String logText) throws IOException {
		if (buildProperties == null || buildProperties.isEmpty()) {
			return NO_OXYGEN_OS;
		}

		String result = NO_OXYGEN_OS;

		// Some keys are not present on all devices. Therefore, we'll need support for multiple keys in a single string.
		// If the first key is not present on this device, try the next key. We split the key string by ", "
		String[] items = itemKeys
				.trim()
				.replace(" ", "")
				.split(",");

		for (String item : items) {

			BufferedReader in = new BufferedReader(new StringReader(buildProperties));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				if (inputLine.contains(item)) {
					// Remove brackets ([ and ]) and ":" from the getprop command output line
					result = inputLine.replace("[" + item + "]: ", "");
					result = result.replace("[", "");
					result = result.replace("]", "");

					// @hack #1 (OS_VERSION_NUMBER_LOOKUP_KEY): OxygenOS 2.0.0 - 3.x sometimes contain incorrect H2OS values for ro.rom.version
					// if this is the case, discard the value and try with the next item in "items" array
					if (item.equals(RO_ROM_VERSION_LOOKUP_KEY) && result.contains(RO_ROM_VERSION_H2OS)) {
						result = NO_OXYGEN_OS;
						continue;
					}

					// @hack #2 (OS_VERSION_NUMBER_LOOKUP_KEY): OnePlus 7 and later store hardcoded "Oxygen OS " in their version number of the firmware.
					// As the app only shows the number or ads custom formatting, remove this prefix
					if (item.equals(RO_ROM_VERSION_LOOKUP_KEY) && result.contains(RO_ROM_VERSION_OXYGENOS_PREFIX)) {
						result = result.replace(RO_ROM_VERSION_OXYGENOS_PREFIX, "");
					}

					// @hack #3 (DEVICE_NAME_LOOKUP_KEY / support for regional device variants): OnePlus 7 Pro and newer devices come in regional variants which cannot be detected by ro.display.series.
					// However, the property that *does* contain it (ro.product.name) does not play nice with values present on older devices.
					// Bypass: if the key is 'ro.display.series' and the value is one of these devices, then read 'ro.product.name' instead to detect the correct device
					if (item.equals(RO_DISPLAY_SERIES_LOOKUP_KEY) && RO_PRODUCT_NAME_LOOKUP_DEVICES.contains(result)) {
						// Android Logger class is not loaded during unit tests, so omit logging if called from test.
						String logMessage = logText != null ? "Detected " + result + " variant: %s" : null;
						result = readBuildPropItem(RO_PRODUCT_NAME_LOOKUP_KEY, buildProperties, logMessage);
					}

					// @hack #4 (BUILD_SOFT_VERSION_LOOKUP_KEY / support for Indian variants): OnePlus 7T-series and newer devices come with India-specific builds
					// which cannot be detected by ro.product.name.
					// However, the property ro.build.soft.version helps distinguish between international and Indian variants.
					// Bypass: if the key is 'ro.product.name' and the value is one of these devices, then read 'ro.build.soft.version' additionally to detect the correct device
					if (item.equals(RO_PRODUCT_NAME_LOOKUP_KEY) && RO_BUILD_SOFT_VERSION_LOOKUP_DEVICES.contains(result)) {
						// Android Logger class is not loaded during unit tests, so omit logging if called from test.
						String logMessage = logText != null ? "Detected " + result + " variant: %s" : null;
						String buildSoftVersion = readBuildPropItem(RO_BUILD_SOFT_VERSION_LOOKUP_KEY, buildProperties, logMessage);

						// append _IN to mark device as Indian variant
						if (buildSoftVersion.charAt(0) == 'I') {
							result += "_IN";
						}
					}

					if (logText != null) {
						logVerbose(TAG, String.format(logText, result));
					}

					return result; // Return the first successfully detected item. This because some keys have multiple values which all exist in the same properties file.
				}
			}
		}
		return result;
	}

	public String getOxygenDeviceName() {
		return oxygenDeviceName;
	}

	public String getOxygenOSVersion() {
		return oxygenOSVersion;
	}

	public String getOxygenOSOTAVersion() {
		return oxygenOSOTAVersion;
	}

	public String getSecurityPatchDate() {
		return securityPatchDate;
	}

	public String getOemFingerprint() {
		return oemFingerprint;
	}

	public boolean isABPartitionLayout() {
		return ABPartitionLayout;
	}
}
