package com.arjanvlek.oxygenupdater.deviceinformation

import android.annotation.SuppressLint
import android.os.Build
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose
import java.io.RandomAccessFile
import java.math.BigDecimal

class DeviceInformationData {

    val deviceManufacturer: String = Build.MANUFACTURER
    val deviceName: String = Build.DEVICE
    val soc: String = Build.BOARD
    val osVersion: String = Build.VERSION.RELEASE
    val incrementalOsVersion: String = Build.VERSION.INCREMENTAL
    val cpuFrequency = calculateCpuFrequency()

    @Suppress("DEPRECATION")
    @SuppressLint("HardwareIds")
    // Only used on older Android versions.
    // Serial number is only used on older Android versions as it requires too much permissions on O and higher.
    val serialNumber: String = if (Build.VERSION.SDK_INT >= 26) UNKNOWN else Build.SERIAL

    fun calculateCpuFrequency(): String {
        return try {
            val cpuFrequencyFileReader = RandomAccessFile(CPU_FREQUENCY_FILE_PATH, "r")
            val cpuFrequencyString = cpuFrequencyFileReader.readLine()

            cpuFrequencyFileReader.close()

            val cpuFrequency = cpuFrequencyString.toInt() / 1000

            // CPU Frequency in GHz
            BigDecimal(cpuFrequency).divide(BigDecimal(1000), 3, BigDecimal.ROUND_DOWN).toString()
        } catch (e: Exception) {
            logVerbose("DeviceInformationData", "CPU Frequency information is not available", e)
            UNKNOWN
        }
    }

    companion object {
        const val UNKNOWN = "-"
        private const val CPU_FREQUENCY_FILE_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"
    }
}
