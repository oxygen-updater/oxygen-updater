package com.oxygenupdater.ui.settings

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.remove
import com.oxygenupdater.extensions.set
import com.oxygenupdater.extensions.setIdAndName
import com.oxygenupdater.internal.NotSet
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyDevice
import com.oxygenupdater.internal.settings.KeyDeviceId
import com.oxygenupdater.internal.settings.KeyThemeId
import com.oxygenupdater.internal.settings.KeyUpdateMethod
import com.oxygenupdater.internal.settings.KeyUpdateMethodId
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.SelectableModel
import com.oxygenupdater.models.SystemVersionProperties.deviceProductName
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.ui.SettingsListConfig
import com.oxygenupdater.ui.Theme
import com.oxygenupdater.utils.FcmUtils
import com.oxygenupdater.utils.logDebug
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val serverRepository: ServerRepository,
    private val fcmUtils: FcmUtils,
    private val crashlytics: FirebaseCrashlytics,
) : ViewModel() {

    private var recommendedDeviceId = NotSetL
    private var recommendedMethodId = NotSetL

    var theme by mutableStateOf(Theme.from(sharedPreferences[KeyThemeId, Theme.System.value]))
        private set

    fun updateTheme(theme: Theme) {
        sharedPreferences[KeyThemeId] = theme.value
        this.theme = theme
    }

    private val deviceConfigFlow = MutableStateFlow(SettingsListConfig(listOf<Device>(), 0, NotSetL))
    private val methodConfigFlow = MutableStateFlow(SettingsListConfig(listOf<UpdateMethod>(), 0, NotSetL))

    val deviceConfigState = deviceConfigFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = deviceConfigFlow.value
    )

    val methodConfigState = methodConfigFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = methodConfigFlow.value
    )

    fun fetchEnabledDevices(cached: List<Device>? = null) = viewModelScope.launch(Dispatchers.IO) {
        val enabledDevices = if (cached.isNullOrEmpty()) {
            serverRepository.fetchDevices(DeviceRequestFilter.Enabled) ?: listOf()
        } else cached

        setup(enabledDevices)
    }

    private fun setup(enabledDevices: List<Device>) = viewModelScope.launch(Dispatchers.IO) {
        var selectedIndex = NotSet
        recommendedDeviceId = NotSetL

        val deviceId = sharedPreferences[KeyDeviceId, NotSetL]
        for ((index, device) in enabledDevices.withIndex()) {
            // Take first match only
            if (selectedIndex != NotSet && recommendedDeviceId != NotSetL) break

            if (deviceId != NotSetL && deviceId == device.id) selectedIndex = index
            if (device.productNames.contains(deviceProductName)) recommendedDeviceId = device.id
        }

        // Fallback to recommended index
        if (selectedIndex == NotSet) selectedIndex = enabledDevices.indexOfFirst {
            it.id == recommendedDeviceId
        }

        deviceConfigFlow.emitIfChanged(
            list = enabledDevices,
            recommendedId = recommendedDeviceId,
            selectedId = if (selectedIndex == NotSet) deviceId else {
                enabledDevices.getOrNull(selectedIndex)?.id ?: NotSetL
            },
        )

        if (selectedIndex != NotSet && selectedIndex < enabledDevices.size) {
            // This call also fetches methods for device
            saveSelectedDevice(enabledDevices[selectedIndex], deviceId, deviceId == NotSetL)
        }
    }

    private fun fetchMethodsForDevice(deviceId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val methods = serverRepository.fetchUpdateMethodsForDevice(deviceId) ?: listOf()
        val methodId = sharedPreferences[KeyUpdateMethodId, NotSetL]
        var selectedIndex = if (methodId == NotSetL) NotSet else methods.indexOfFirst {
            it.id == methodId
        }

        val rooted = Shell.isAppGrantedRoot() == true
        recommendedMethodId = methods.findLast {
            if (rooted) it.recommendedForRootedDevice else it.recommendedForNonRootedDevice
        }?.id ?: NotSetL

        // Fallback to recommended index
        if (selectedIndex == NotSet) selectedIndex = methods.indexOfFirst {
            it.id == recommendedMethodId
        }

        methodConfigFlow.emitIfChanged(
            list = methods,
            recommendedId = recommendedMethodId,
            selectedId = if (selectedIndex == NotSet) methodId else {
                methods.getOrNull(selectedIndex)?.id ?: NotSetL
            },
        )

        if (selectedIndex != NotSet && selectedIndex < methods.size) {
            saveSelectedMethod(methods[selectedIndex], methodId == NotSetL)
        }
    }

    /**
     * Saves device ID & name in [SharedPreferences].
     * Additionally, refreshes [methodConfigFlow] via [fetchMethodsForDevice].
     */
    fun saveSelectedDevice(
        device: Device,
        oldId: Long = sharedPreferences[KeyDeviceId, NotSetL],
        persist: Boolean = true,
    ) {
        val id = device.id

        // Clear methods if device changed
        if (oldId != NotSetL && oldId != id) {
            logDebug(TAG, "Device changed ($oldId -> $id); clearing saved method")
            methodConfigFlow.tryEmit(SettingsListConfig(listOf(), recommendedMethodId, NotSetL))
            sharedPreferences.remove(KeyUpdateMethodId, KeyUpdateMethod)
        }

        if (persist) {
            logDebug(TAG, "Persisting device #$id: ${device.name}")
            // Persist only if there's no device saved yet (most likely first-launch)
            sharedPreferences.setIdAndName(KeyDevice, id, device.name)

            deviceConfigFlow.emitIfChanged(
                recommendedId = recommendedDeviceId,
                selectedId = id,
            )
        }

        fetchMethodsForDevice(id)
    }

    /**
     * Saves method ID & name in [SharedPreferences].
     * Additionally, updates [FirebaseCrashlytics] user identifier.
     */
    fun saveSelectedMethod(
        method: UpdateMethod,
        persist: Boolean = true,
    ) {
        val id = method.id

        if (persist) {
            logDebug(TAG, "Persisting method #$id: ${method.name}")
            // Persist only if there's no method saved yet (most likely first-launch)
            sharedPreferences.setIdAndName(KeyUpdateMethod, id, method.name)

            methodConfigFlow.emitIfChanged(
                recommendedId = recommendedMethodId,
                selectedId = id,
            )
        }

        updateCrashlyticsUserId()
    }

    fun updateCrashlyticsUserId() {
        val device = sharedPreferences[KeyDevice, "<UNKNOWN>"]
        val method = sharedPreferences[KeyUpdateMethod, "<UNKNOWN>"]
        crashlytics.setUserId("Device: $device, Update Method: $method")
    }

    fun resubscribeToFcmTopic() = viewModelScope.launch(Dispatchers.IO) {
        fcmUtils.resubscribe()
    }

    private fun <T : SelectableModel> MutableStateFlow<SettingsListConfig<T>>.emitIfChanged(
        list: List<T> = value.list,
        recommendedId: Long,
        selectedId: Long,
    ) {
        if (list == value.list && recommendedId == value.recommendedId && selectedId == value.selectedId) return

        tryEmit(SettingsListConfig(list, recommendedId, selectedId))
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
