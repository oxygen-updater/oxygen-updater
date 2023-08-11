package com.oxygenupdater.compose.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.compose.ui.SettingsListWrapper
import com.oxygenupdater.compose.ui.onboarding.NOT_SET
import com.oxygenupdater.compose.ui.onboarding.NOT_SET_L
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.repositories.ServerRepository
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

    var initialDeviceIndex = NOT_SET
    var initialMethodIndex = NOT_SET
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
        initialDeviceIndex = NOT_SET
        deviceName = null

        val deviceId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, NOT_SET_L)
        for ((index, device) in enabledDevices.withIndex()) {
            if (initialDeviceIndex != NOT_SET && deviceName != null) break // take first match only

            var matched: Boolean? = null  // save computation for future use
            if (deviceName == null) { // take first match only
                matched = device.productNames.contains(SystemVersionProperties.oxygenDeviceName)
                if (matched) deviceName = device.name
            }

            if (
                (deviceId != NOT_SET_L && deviceId == device.id) ||
                (matched ?: device.productNames.contains(SystemVersionProperties.oxygenDeviceName))
            ) initialDeviceIndex = index
        }

        if (initialDeviceIndex != NOT_SET && enabledDevices.size > initialDeviceIndex) {
            // Persist only if there's no device saved yet. This call also fetches methods for device.
            saveSelectedDevice(enabledDevices[initialDeviceIndex], deviceId == NOT_SET_L)
        }
    }

    private fun fetchMethodsForDevice(deviceId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val methods = serverRepository.fetchUpdateMethodsForDevice(deviceId) ?: listOf()
        val methodId = PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, NOT_SET_L)
        val rooted = Shell.isAppGrantedRoot() == true
        initialMethodIndex = if (methodId != NOT_SET_L) methods.indexOfFirst {
            it.id == methodId
        } else methods.indexOfLast {
            if (rooted) it.recommendedForRootedDevice else it.recommendedForNonRootedDevice
        }

        if (initialMethodIndex != NOT_SET && methods.size > initialMethodIndex) {
            // Persist only if there's no method saved yet
            saveSelectedMethod(methods[initialMethodIndex], methodId == NOT_SET_L)
        }

        methodsForDeviceFlow.emit(methods)
    }

    /**
     * Saves device ID & name in [android.content.SharedPreferences].
     * Additionally, refreshes [methodsForDeviceFlow] via [fetchMethodsForDevice].
     */
    fun saveSelectedDevice(device: Device, persist: Boolean = false) {
        val id = device.id

        // Clear methods if device changed
        val oldId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, NOT_SET_L)
        if (oldId != NOT_SET_L && oldId != id) methodsForDeviceFlow.value = listOf()

        if (persist) {
            PrefManager.putLong(PrefManager.PROPERTY_DEVICE_ID, id)
            PrefManager.putString(PrefManager.PROPERTY_DEVICE, device.name)
        }

        fetchMethodsForDevice(id)
    }

    /**
     * Saves method ID & name in [android.content.SharedPreferences].
     * Additionally, updates [FirebaseCrashlytics]' user identifier.
     */
    fun saveSelectedMethod(method: UpdateMethod, persist: Boolean = false) {
        if (persist) {
            PrefManager.putLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, method.id)
            PrefManager.putString(PrefManager.PROPERTY_UPDATE_METHOD, method.name)
        }

        updateCrashlyticsUserId()
    }

    fun updateCrashlyticsUserId() {
        val device = PrefManager.getString(PrefManager.PROPERTY_DEVICE, "<UNKNOWN>")
        val method = PrefManager.getString(PrefManager.PROPERTY_UPDATE_METHOD, "<UNKNOWN>")
        crashlytics.setUserId("Device: $device, Update Method: $method")
    }

    fun subscribeToNotificationTopics(enabledDevices: List<Device>) = viewModelScope.launch(Dispatchers.IO) {
        if (enabledDevices.isEmpty()) return@launch

        NotificationTopicSubscriber.resubscribe(
            enabledDevices,
            serverRepository.fetchAllMethods() ?: listOf()
        )
    }
}
