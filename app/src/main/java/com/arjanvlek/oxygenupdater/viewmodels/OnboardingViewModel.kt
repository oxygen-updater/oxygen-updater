package com.arjanvlek.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.models.UpdateMethod
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import com.arjanvlek.oxygenupdater.utils.RootAccessChecker
import com.crashlytics.android.Crashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.arjanvlek.oxygenupdater.activities.OnboardingActivity]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class OnboardingViewModel(
    private val serverRepository: ServerRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _allDevices = MutableLiveData<List<Device>>()

    private val _enabledDevices = MutableLiveData<List<Device>>()
    val enabledDevices: LiveData<List<Device>>
        get() = _enabledDevices

    private val _updateMethodsForDevice = MutableLiveData<List<UpdateMethod>>()

    private val _selectedDevice = MutableLiveData<Device>()
    val selectedDevice: LiveData<Device>
        get() = _selectedDevice

    private val _selectedUpdateMethod = MutableLiveData<UpdateMethod>()
    val selectedUpdateMethod: LiveData<UpdateMethod>
        get() = _selectedUpdateMethod

    /**
     * Fetches all devices and posts both [_allDevices] and [_enabledDevices].
     * This function is used directly in [com.arjanvlek.oxygenupdater.activities.OnboardingActivity],
     * whereas [enabledDevices] is used in [com.arjanvlek.oxygenupdater.fragments.DeviceChooserOnboardingFragment]
     */
    fun fetchAllDevices(): LiveData<List<Device>> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchDevices(DeviceRequestFilter.ALL, false).also { allDevices ->
            _allDevices.postValue(allDevices)
            _enabledDevices.postValue(allDevices.filter { it.enabled })
        }
    }.let { _allDevices }

    fun fetchUpdateMethodsForDevice(deviceId: Long): LiveData<List<UpdateMethod>> = RootAccessChecker.checkRootAccess { hasRootAccess ->
        viewModelScope.launch(Dispatchers.IO) {
            _updateMethodsForDevice.postValue(serverRepository.fetchUpdateMethodsForDevice(deviceId, hasRootAccess))
        }
    }.let { _updateMethodsForDevice }

    /**
     * Post [_selectedDevice] and save the ID & name in [android.content.SharedPreferences]
     */
    fun updateSelectedDevice(device: Device) = settingsManager.let {
        _selectedDevice.postValue(device)

        it.savePreference(SettingsManager.PROPERTY_DEVICE_ID, device.id)
        it.savePreference(SettingsManager.PROPERTY_DEVICE, device.name)
    }

    /**
     * Post [_selectedUpdateMethod] and save the ID & name in [android.content.SharedPreferences].
     * Additionally, update [Crashlytics]' user identifier
     */
    fun updateSelectedUpdateMethod(updateMethod: UpdateMethod) = settingsManager.let {
        _selectedUpdateMethod.postValue(updateMethod)

        it.savePreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, updateMethod.id)
        it.savePreference(SettingsManager.PROPERTY_UPDATE_METHOD, updateMethod.name)

        // since both device and update method have been selected,
        // we can safely update Crashlytics' user identifier
        Crashlytics.setUserIdentifier(
            "Device: " + it.getPreference(SettingsManager.PROPERTY_DEVICE, "<UNKNOWN>")
                    + ", Update Method: " + it.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "<UNKNOWN>")
        )
    }
}
