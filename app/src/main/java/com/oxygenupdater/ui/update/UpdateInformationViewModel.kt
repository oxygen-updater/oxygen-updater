package com.oxygenupdater.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.ui.RefreshAwareState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class UpdateInformationViewModel(private val serverRepository: ServerRepository) : ViewModel() {

    private var previousDeviceId = NotSetL
    private var previousMethodId = NotSetL
    private val refreshingFlow = MutableStateFlow(true)

    val state = refreshingFlow.combine(serverRepository.updateDataFlow) { refreshing, updateData ->
        RefreshAwareState(refreshing, updateData)
    }.distinctUntilChanged()

    init {
        refresh()
    }

    fun refresh(deviceId: Long? = null, methodId: Long? = null) = viewModelScope.launch(Dispatchers.IO) {
        previousDeviceId = deviceId ?: PrefManager.getLong(PrefManager.KeyDeviceId, NotSetL)
        previousMethodId = methodId ?: PrefManager.getLong(PrefManager.KeyUpdateMethodId, NotSetL)

        refreshingFlow.value = true
        serverRepository.fetchUpdateData(previousDeviceId, previousMethodId)
        refreshingFlow.value = false
    }

    /**
     * Calls [refresh] only if device or method ID changed from the last invocation.
     * This can happen if user changed settings and then came back to this screen.
     */
    fun refreshIfNeeded() {
        val deviceId = PrefManager.getLong(PrefManager.KeyDeviceId, NotSetL)
        val methodId = PrefManager.getLong(PrefManager.KeyUpdateMethodId, NotSetL)
        if (previousDeviceId != deviceId || previousMethodId != methodId) refresh(deviceId, methodId)
    }
}
