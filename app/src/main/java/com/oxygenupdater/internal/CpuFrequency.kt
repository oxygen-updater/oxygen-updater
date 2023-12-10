package com.oxygenupdater.internal

import com.oxygenupdater.utils.logVerbose
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

val CpuFrequency = try {
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
    logVerbose("CpuFrequency", "CPU Frequency information is not available", e)
    null
}

private const val CpuFreqPathPrefix = "/sys/devices/system/cpu/cpu"
private const val CpuFreqPathSuffix = "/cpufreq/cpuinfo_max_freq"
private fun cpuFreq(index: Int) = try {
    val file = File(CpuFreqPathPrefix + index + CpuFreqPathSuffix)
    if (file.canRead()) BigDecimal(file.readText().trim()).divide(
        BigDecimal(1_000_000), 6, RoundingMode.HALF_EVEN
    ).stripTrailingZeros() else BigDecimal.ZERO
} catch (e: Exception) {
    logVerbose("CpuFrequency", "CPU Frequency information is not available", e)
    BigDecimal.ZERO
}
