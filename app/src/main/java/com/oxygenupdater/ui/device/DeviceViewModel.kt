package com.oxygenupdater.ui.device

import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.utils.logVerbose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor() : ViewModel() {

    private val cpuFrequenciesFlow = MutableStateFlow<ScatterMap<Float, Int>>(emptyScatterMap())
    val cpuFrequencies = cpuFrequenciesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyScatterMap(),
    )

    init {
        buildCpuFrequencyList()
    }

    private fun buildCpuFrequencyList() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val numCpus = Runtime.getRuntime().availableProcessors()
            MutableScatterMap<Float, Int>((numCpus / 2).fastCoerceAtLeast(1)).apply {
                // Collect & group max frequencies of all CPUs
                repeat(numCpus) { index ->
                    // Cooperative cancellation: cancel if Job isn't active. Check before reading
                    // the file in each loop iteration. This isn't needed if using `withContext`
                    // or other `kotlinx.coroutines` suspend functions.
                    ensureActive()

                    val freq = try {
                        // This is the main part we're offloading to an IO thread
                        val file = File(PathPrefix + index + CpuInfoMaxFreqPathSuffix)
                        file.readText().trim().ifEmpty { null }?.toLongOrNull()?.div(1_000_000f) ?: 0f
                    } catch (e: Exception) {
                        logVerbose(TAG, "CPU Frequency information is not available", e)
                        0f
                    }

                    if (freq == 0f) return@repeat
                    compute(freq) { _, value -> if (value == null) 1 else value + 1 }
                }
            }.let {
                if (it.isNotEmpty()) cpuFrequenciesFlow.emit(it)
            }
        } catch (e: Exception) {
            logVerbose(TAG, "Could not collect CPU frequencies", e)
        }
    }

    companion object {
        private const val TAG = "DeviceViewModel"
        private const val PathPrefix = "/sys/devices/system/cpu/cpu"
        private const val CpuInfoMaxFreqPathSuffix = "/cpufreq/cpuinfo_max_freq"
    }
}
