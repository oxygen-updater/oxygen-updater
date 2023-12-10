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
import com.oxygenupdater.models.SystemVersionProperties.oxygenDeviceName
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

    private var initialDeviceIndex = NotSet
    private var initialMethodIndex = NotSet

    var deviceName: String? = null
        private set

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
        var selectedDeviceIndex = NotSet
        initialDeviceIndex = NotSet
        deviceName = null

        val deviceId = sharedPreferences[KeyDeviceId, NotSetL]
        for ((index, device) in enabledDevices.withIndex()) {
            // Take first match only
            if (selectedDeviceIndex != NotSet && initialDeviceIndex != NotSet && deviceName != null) break

            var matched: Boolean? = null  // save computation for future use
            if (deviceName == null) { // take first match only
                matched = device.productNames.contains(oxygenDeviceName)
                if (matched) deviceName = device.name
            }

            if (deviceId != NotSetL && deviceId == device.id) selectedDeviceIndex = index
            if (matched ?: device.productNames.contains(oxygenDeviceName)) initialDeviceIndex = index
        }

        // If there's only one device, select it, otherwise fallback to initial index
        val size = enabledDevices.size
        if (selectedDeviceIndex == NotSet) selectedDeviceIndex = if (size == 1) 0 else initialDeviceIndex

        deviceConfigFlow.emitIfChanged(
            list = enabledDevices,
            initialIndex = initialDeviceIndex,
            selectedId = if (selectedDeviceIndex == NotSet) deviceId else {
                enabledDevices.getOrNull(selectedDeviceIndex)?.id ?: NotSetL
            },
        )

        if (selectedDeviceIndex != NotSet && selectedDeviceIndex < size) {
            // This call also fetches methods for device
            saveSelectedDevice(enabledDevices[selectedDeviceIndex], deviceId, deviceId == NotSetL)
        }
    }

    private fun fetchMethodsForDevice(deviceId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val methods = serverRepository.fetchUpdateMethodsForDevice(deviceId) ?: listOf()
        val methodId = sharedPreferences[KeyUpdateMethodId, NotSetL]
        var selectedMethodIndex = if (methodId == NotSetL) NotSet else methods.indexOfFirst {
            it.id == methodId
        }

        val rooted = Shell.isAppGrantedRoot() == true
        initialMethodIndex = methods.indexOfLast {
            if (rooted) it.recommendedForRootedDevice else it.recommendedForNonRootedDevice
        }

        // If there's only one method, select it, otherwise fallback to initial index
        val size = methods.size
        if (selectedMethodIndex == NotSet) selectedMethodIndex = if (size == 1) 0 else initialMethodIndex

        methodConfigFlow.emitIfChanged(
            list = methods,
            initialIndex = initialMethodIndex,
            selectedId = if (selectedMethodIndex == NotSet) methodId else {
                methods.getOrNull(selectedMethodIndex)?.id ?: NotSetL
            },
        )

        if (selectedMethodIndex != NotSet && selectedMethodIndex < size) {
            saveSelectedMethod(methods[selectedMethodIndex], methodId == NotSetL)
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
            methodConfigFlow.tryEmit(SettingsListConfig(listOf(), initialMethodIndex, NotSetL))
            sharedPreferences.remove(KeyUpdateMethodId, KeyUpdateMethod)
        }

        if (persist) {
            logDebug(TAG, "Persisting device #$id: ${device.name}")
            // Persist only if there's no device saved yet (most likely first-launch)
            sharedPreferences.setIdAndName(KeyDevice, id, device.name)

            deviceConfigFlow.emitIfChanged(
                initialIndex = initialDeviceIndex,
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
                initialIndex = initialMethodIndex,
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
        initialIndex: Int,
        selectedId: Long,
    ) {
        if (list == value.list && initialIndex == value.initialIndex && selectedId == value.selectedId) return

        tryEmit(SettingsListConfig(list, initialIndex, selectedId))
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
