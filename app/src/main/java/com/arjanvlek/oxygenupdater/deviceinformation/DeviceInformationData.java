package com.arjanvlek.oxygenupdater.deviceinformation;

import android.annotation.SuppressLint;
import android.os.Build;

import java.io.RandomAccessFile;
import java.math.BigDecimal;

import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose;

@SuppressWarnings("WeakerAccess")
public class DeviceInformationData {
	public static final String UNKNOWN = "-";
	private static final String CPU_FREQUENCY_FILE_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";
	private final String deviceManufacturer;
	private final String deviceName;
	private final String soc;
	private final String cpuFrequency;
	private final String osVersion;
	private final String incrementalOsVersion;
	private final String serialNumber;

	@SuppressLint("HardwareIds") // Only used on older Android versions.
	public DeviceInformationData() {
		deviceManufacturer = Build.MANUFACTURER;
		deviceName = Build.DEVICE;
		soc = Build.BOARD;
		osVersion = Build.VERSION.RELEASE;
		incrementalOsVersion = Build.VERSION.INCREMENTAL;
		serialNumber = Build.VERSION.SDK_INT >= 26 ? UNKNOWN : Build.SERIAL; // Serial number is only used on older Android versions as it requires too much permissions on O and higher.
		cpuFrequency = calculateCpuFrequency();
	}

	public String getDeviceManufacturer() {
		return deviceManufacturer;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public String getSoc() {
		return soc;
	}

	public String getCpuFrequency() {
		return cpuFrequency;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public String getIncrementalOsVersion() {
		return incrementalOsVersion;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public String calculateCpuFrequency() {
		try {
			RandomAccessFile cpuFrequencyFileReader = new RandomAccessFile(CPU_FREQUENCY_FILE_PATH, "r");
			String cpuFrequencyString = cpuFrequencyFileReader.readLine();

			cpuFrequencyFileReader.close();

			int cpuFrequency = Integer.parseInt(cpuFrequencyString);
			cpuFrequency = cpuFrequency / 1000;

			BigDecimal cpuFrequencyGhz = new BigDecimal(cpuFrequency).divide(new BigDecimal(1000), 3, BigDecimal.ROUND_DOWN);
			return cpuFrequencyGhz.toString();
		} catch (Exception e) {
			logVerbose("DeviceInformationData", "CPU Frequency information is not available", e);
			return UNKNOWN;
		}
	}
}
