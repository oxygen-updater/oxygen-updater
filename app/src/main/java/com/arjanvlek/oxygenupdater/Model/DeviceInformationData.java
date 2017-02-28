package com.arjanvlek.oxygenupdater.Model;

import android.os.Build;

import com.arjanvlek.oxygenupdater.support.Logger;

import java.io.RandomAccessFile;
import java.math.BigDecimal;

public class DeviceInformationData {
    private final String deviceManufacturer;
    private final String deviceName;
    private final String soc;
    private final String cpuFrequency;
    private final String osVersion;
    private final String incrementalOsVersion;
    private final String serialNumber;

    private static final String CPU_FREQUENCY_FILE_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";
    public static final String UNKNOWN = "-";

    public DeviceInformationData() {
        this.deviceManufacturer = Build.MANUFACTURER;
        this.deviceName = Build.DEVICE;
        this.soc = Build.BOARD;
        this.osVersion = Build.VERSION.RELEASE;
        this.incrementalOsVersion = Build.VERSION.INCREMENTAL;
        this.serialNumber = Build.SERIAL;
        this.cpuFrequency = calculateCpuFrequency();
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
            Logger.logVerbose("DeviceInformationData", "CPU Frequency information is not available: " ,e);
            return UNKNOWN;
        }
    }
}
