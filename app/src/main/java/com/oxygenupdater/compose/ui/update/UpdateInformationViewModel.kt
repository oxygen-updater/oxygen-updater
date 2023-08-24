package com.oxygenupdater.compose.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class UpdateInformationViewModel(private val serverRepository: ServerRepository) : ViewModel() {

    private val refreshingFlow = MutableStateFlow(true)

    val state = refreshingFlow.combine(serverRepository.updateDataFlow) { refreshing, updateData ->
        RefreshAwareState(refreshing, updateData)
    }.distinctUntilChanged()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        refreshingFlow.emit(true)
        serverRepository.fetchUpdateData()
        refreshingFlow.emit(false)
    }
}
