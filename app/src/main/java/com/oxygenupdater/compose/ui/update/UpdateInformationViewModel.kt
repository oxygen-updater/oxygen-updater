package com.oxygenupdater.compose.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpdateInformationViewModel(private val serverRepository: ServerRepository) : ViewModel() {

    private val refreshingFlow = MutableStateFlow(true)
    private val flow = MutableStateFlow(serverRepository.fetchUpdateDataFromPrefs())

    val state = refreshingFlow.combine(flow) { refreshing, updateData ->
        RefreshAwareState(refreshing, updateData)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RefreshAwareState(refreshingFlow.value, flow.value)
    )

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        val deviceId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        refreshingFlow.emit(true)
        flow.emit(serverRepository.fetchUpdateData(deviceId, updateMethodId, SystemVersionProperties.oxygenOSOTAVersion))
        refreshingFlow.emit(false)
    }
}
