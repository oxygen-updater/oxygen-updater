package com.oxygenupdater.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.internal.NotSet
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.SystemVersionProperties.oxygenDeviceName
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.ui.SettingsListWrapper
import com.oxygenupdater.utils.NotificationTopicSubscriber
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val serverRepository: ServerRepository,
    private val crashlytics: FirebaseCrashlytics,
) : ViewModel() {

    var initialDeviceIndex = NotSet
    var initialMethodIndex = NotSet
    var deviceName: String? = null

    private val enabledDevicesFlow = MutableStateFlow<List<Device>>(listOf())
    private val methodsForDeviceFlow = MutableStateFlow<List<UpdateMethod>>(listOf())

    val state = enabledDevicesFlow.combine(methodsForDeviceFlow) { devices, methods ->
        SettingsListWrapper(devices, methods)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsListWrapper(enabledDevicesFlow.value, methodsForDeviceFlow.value)
    )

    fun fetchEnabledDevices(cached: List<Device>? = null) = viewModelScope.launch(Dispatchers.IO) {
        val enabledDevices = if (cached.isNullOrEmpty()) {
            serverRepository.fetchDevices(DeviceRequestFilter.Enabled) ?: listOf()
        } else cached

        setup(enabledDevices)
        enabledDevicesFlow.emit(enabledDevices)
    }

    private fun setup(enabledDevices: List<Device>) = viewModelScope.launch(Dispatchers.IO) {
        var selectedDeviceIndex = NotSet
        initialDeviceIndex = NotSet
        deviceName = null

        val deviceId = PrefManager.getLong(PrefManager.KeyDeviceId, NotSetL)
        for ((index, device) in enabledDevices.withIndex()) {
            if (selectedDeviceIndex != NotSet && initialDeviceIndex != NotSet && deviceName != null) break // take first match only

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

        if (selectedDeviceIndex != NotSet && selectedDeviceIndex < size) {
            // Persist only if there's no device saved yet. This call also fetches methods for device.
            saveSelectedDevice(enabledDevices[selectedDeviceIndex], deviceId == NotSetL)
        }
    }

    private fun fetchMethodsForDevice(deviceId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val methods = serverRepository.fetchUpdateMethodsForDevice(deviceId) ?: listOf()
        val methodId = PrefManager.getLong(PrefManager.KeyUpdateMethodId, NotSetL)
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

        if (selectedMethodIndex != NotSet && selectedMethodIndex < size) {
            // Persist only if there's no method saved yet
            saveSelectedMethod(methods[selectedMethodIndex], methodId == NotSetL)
        }

        methodsForDeviceFlow.emit(methods)
    }

    /**
     * Saves device ID & name in [android.content.SharedPreferences].
     * Additionally, refreshes [methodsForDeviceFlow] via [fetchMethodsForDevice].
     */
    fun saveSelectedDevice(device: Device, persist: Boolean = true) {
        val id = device.id

        // Clear methods if device changed
        val oldId = PrefManager.getLong(PrefManager.KeyDeviceId, NotSetL)
        if (oldId != NotSetL && oldId != id) {
            methodsForDeviceFlow.value = listOf()
            PrefManager.remove(PrefManager.KeyUpdateMethodId)
            PrefManager.remove(PrefManager.KeyUpdateMethod)
        }

        if (persist) {
            PrefManager.putLong(PrefManager.KeyDeviceId, id)
            PrefManager.putString(PrefManager.KeyDevice, device.name)
        }

        fetchMethodsForDevice(id)
    }

    /**
     * Saves method ID & name in [android.content.SharedPreferences].
     * Additionally, updates [FirebaseCrashlytics]' user identifier.
     */
    fun saveSelectedMethod(method: UpdateMethod, persist: Boolean = true) {
        if (persist) {
            PrefManager.putLong(PrefManager.KeyUpdateMethodId, method.id)
            PrefManager.putString(PrefManager.KeyUpdateMethod, method.name)
        }

        updateCrashlyticsUserId()
    }

    fun updateCrashlyticsUserId() {
        val device = PrefManager.getString(PrefManager.KeyDevice, "<UNKNOWN>")
        val method = PrefManager.getString(PrefManager.KeyUpdateMethod, "<UNKNOWN>")
        crashlytics.setUserId("Device: $device, Update Method: $method")
    }

    fun subscribeToNotificationTopics(enabledDevices: List<Device>?) = viewModelScope.launch(Dispatchers.IO) {
        if (enabledDevices.isNullOrEmpty()) return@launch

        NotificationTopicSubscriber.resubscribe(
            enabledDevices,
            serverRepository.fetchAllMethods() ?: listOf()
        )
    }
}
