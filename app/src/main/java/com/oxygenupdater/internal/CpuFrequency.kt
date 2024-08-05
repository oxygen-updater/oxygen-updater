package com.oxygenupdater.internal

import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap
import com.oxygenupdater.utils.logVerbose
import java.io.File

private const val TAG = "CpuFrequency"
private const val PathPrefix = "/sys/devices/system/cpu/cpu"
private const val CpuInfoMaxFreqPathSuffix = "/cpufreq/cpuinfo_max_freq"

private var _cpuFrequencies: ScatterMap<Float, Int>? = null
val CpuFrequencies: ScatterMap<Float, Int>
    get() {
        if (_cpuFrequencies != null) return _cpuFrequencies!!
        _cpuFrequencies = try {
            val numCpus = Runtime.getRuntime().availableProcessors()
            if (numCpus == 1) cpuFreq(0).let {
                if (it == 0f) return@let emptyScatterMap()
                MutableScatterMap<Float, Int>(1).apply { put(it, 1) }
            } else MutableScatterMap<Float, Int>(numCpus / 2).apply {
                // Collect & group max frequencies of all CPUs
                repeat(numCpus) {
                    val freq = cpuFreq(it)
                    if (freq == 0f) return@repeat
                    compute(freq) { _, value -> if (value == null) 1 else value + 1 }
                }
            }.let {
                if (it.isEmpty()) emptyScatterMap() else it
            }
        } catch (e: Exception) {
            logVerbose(TAG, "Could not collect CPU frequencies", e)
            null
        }
        return _cpuFrequencies!!
    }


private fun cpuFreq(index: Int) = try {
    val file = File(PathPrefix + index + CpuInfoMaxFreqPathSuffix)
    file.readText().trim().ifEmpty { null }?.toLongOrNull()?.div(1_000_000f) ?: 0f
} catch (e: Exception) {
    logVerbose(TAG, "CPU Frequency information is not available", e)
    0f
}

