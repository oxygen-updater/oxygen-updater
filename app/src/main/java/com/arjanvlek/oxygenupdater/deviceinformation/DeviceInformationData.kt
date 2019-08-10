package com.arjanvlek.oxygenupdater.deviceinformation

import android.annotation.SuppressLint
import android.os.Build
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose
import lombok.Getter
import java.io.RandomAccessFile
import java.math.BigDecimal

@Getter
class DeviceInformationData @SuppressLint("HardwareIds") // Only used on older Android versions.
constructor() {
    val deviceManufacturer: String = Build.MANUFACTURER
    val deviceName: String = Build.DEVICE
    val soc: String = Build.BOARD
    val cpuFrequency: String = calculateCpuFrequency()
    val osVersion: String = Build.VERSION.RELEASE
    val incrementalOsVersion: String = Build.VERSION.INCREMENTAL
    val serialNumber: String = if (Build.VERSION.SDK_INT >= 26) UNKNOWN else Build.SERIAL

    private fun calculateCpuFrequency(): String {
        return try {
            val cpuFrequencyFileReader = RandomAccessFile(CPU_FREQUENCY_FILE_PATH, "r")
            val cpuFrequencyString = cpuFrequencyFileReader.readLine()

            cpuFrequencyFileReader.close()

            var cpuFrequency = Integer.parseInt(cpuFrequencyString)
            cpuFrequency /= 1000

            val cpuFrequencyGhz = BigDecimal(cpuFrequency).divide(BigDecimal(1000), 3, BigDecimal.ROUND_DOWN)
            cpuFrequencyGhz.toString()
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
