package com.oxygenupdater.ui.update

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.extensions.get
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyDeviceId
import com.oxygenupdater.internal.settings.KeyUpdateMethodId
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.ui.RefreshAwareState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateInformationViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val serverRepository: ServerRepository,
) : ViewModel() {

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
        previousDeviceId = deviceId ?: sharedPreferences[KeyDeviceId, NotSetL]
        previousMethodId = methodId ?: sharedPreferences[KeyUpdateMethodId, NotSetL]

        /**
         * Skip server request if required parameters are invalid. This can happen during onboarding, because that's
         * a part of [com.oxygenupdater.activities.MainActivity] and [ViewModel]s are created globally (not scoped).
         */
        if (previousDeviceId == NotSetL || previousMethodId == NotSetL) return@launch

        refreshingFlow.value = true
        serverRepository.fetchUpdateData(previousDeviceId, previousMethodId)
        refreshingFlow.value = false
    }

    /**
     * Calls [refresh] only if device or method ID changed from the last invocation.
     * This can happen if user changed settings and then came back to this screen.
     */
    fun refreshIfNeeded() {
        val deviceId = sharedPreferences[KeyDeviceId, NotSetL]
        val methodId = sharedPreferences[KeyUpdateMethodId, NotSetL]
        if (previousDeviceId != deviceId || previousMethodId != methodId) refresh(deviceId, methodId)
    }
}
