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
    val soc: String = Build.BOARD.let { board ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socManufacturer = Build.SOC_MANUFACTURER
            val socModel = Build.SOC_MODEL
            val validManufacturer = socManufacturer != Build.UNKNOWN
            val validModel = socModel != Build.UNKNOWN

            if (validManufacturer && validModel) {
                "$socManufacturer $socModel ($board)"
            } else if (validManufacturer) {
                "$socManufacturer ($board)"
            } else if (validModel) {
                "$socModel ($board)"
            } else board
        } else board
    }
    val osVersion: String = Build.VERSION.RELEASE
    val incrementalOsVersion: String = Build.VERSION.INCREMENTAL

    @Suppress("DEPRECATION")
    @SuppressLint("HardwareIds")
    // Only used on older Android versions.
    // Serial number is only used on older Android versions as it requires too much permissions on O and higher.
    val serialNumber = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) Build.SERIAL else null

    val cpuFrequency = try {
        val firstCpuFreq = cpuFreq(0)
        val lastCpuIndex = Runtime.getRuntime().availableProcessors() - 1
        // Skip if single core
        (if (lastCpuIndex == 0) firstCpuFreq else {
            val lastCpuFreq = cpuFreq(lastCpuIndex)
            // Choose higher frequency of the two
            if (firstCpuFreq >= lastCpuFreq) firstCpuFreq else lastCpuFreq
        }).takeIf {
            it != BigDecimal.ZERO
        }
    } catch (e: Exception) {
        logVerbose("DeviceInformationData", "CPU Frequency information is not available", e)
        null
    }

    private const val CpuFreqPathPrefix = "/sys/devices/system/cpu/cpu"
    private const val CpuFreqPathSuffix = "/cpufreq/cpuinfo_max_freq"
    private fun cpuFreq(index: Int) = try {
        val file = File(CpuFreqPathPrefix + index + CpuFreqPathSuffix)
        if (file.canRead()) BigDecimal(file.readText().trim()).divide(
            BigDecimal(1000000), 6, RoundingMode.HALF_EVEN
        ).stripTrailingZeros() else BigDecimal.ZERO
    } catch (e: Exception) {
        logVerbose("DeviceInformationData", "CPU Frequency information is not available", e)
        BigDecimal.ZERO
    }
}
