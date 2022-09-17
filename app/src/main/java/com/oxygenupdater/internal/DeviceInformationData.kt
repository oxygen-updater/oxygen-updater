package com.oxygenupdater.internal

import android.annotation.SuppressLint
import android.os.Build
import com.oxygenupdater.utils.Logger.logVerbose
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

object DeviceInformationData {

    const val UNKNOWN = "-"

    val deviceManufacturer: String = Build.MANUFACTURER
    val deviceName: String = Build.DEVICE
    val model: String = Build.MODEL
    val soc: String = Build.BOARD
    val osVersion: String = Build.VERSION.RELEASE
    val incrementalOsVersion: String = Build.VERSION.INCREMENTAL

    @Suppress("DEPRECATION")
    @SuppressLint("HardwareIds")
    // Only used on older Android versions.
    // Serial number is only used on older Android versions as it requires too much permissions on O and higher.
    val serialNumber: String = if (Build.VERSION.SDK_INT >= 26) UNKNOWN else Build.SERIAL

    val cpuFrequency = try {
        val firstCpuFreq = cpuFreq(0)
        val lastCpuIndex = Runtime.getRuntime().availableProcessors() - 1
        // Skip if single core
        (if (lastCpuIndex == 0) firstCpuFreq else {
            val lastCpuFreq = cpuFreq(lastCpuIndex)
            // Choose higher frequency of the two
            if (firstCpuFreq >= lastCpuFreq) firstCpuFreq else lastCpuFreq
        }).let {
            if (it != BigDecimal.ZERO) it.toString() else UNKNOWN
        }
    } catch (e: Exception) {
        logVerbose("DeviceInformationData", "CPU Frequency information is not available", e)
        UNKNOWN
    }

    private const val CPUFREQ_PATH_PREFIX = "/sys/devices/system/cpu/cpu"
    private const val CPUFREQ_PATH_SUFFIX = "/cpufreq/cpuinfo_max_freq"
    private fun cpuFreq(index: Int) = try {
        val file = File(CPUFREQ_PATH_PREFIX + index + CPUFREQ_PATH_SUFFIX)
        if (file.canRead()) BigDecimal(file.readText().trim()).divide(
            BigDecimal(1000000), 6, RoundingMode.HALF_EVEN
        ).stripTrailingZeros() else BigDecimal.ZERO
    } catch (e: Exception) {
        logVerbose("DeviceInformationData", "CPU Frequency information is not available", e)
        BigDecimal.ZERO
    }
}
