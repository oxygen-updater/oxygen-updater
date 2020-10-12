package com.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.NotificationTopicSubscriber
import com.oxygenupdater.utils.RootAccessChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.oxygenupdater.activities.OnboardingActivity]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class OnboardingViewModel(
    private val serverRepository: ServerRepository,
    private val crashlytics: FirebaseCrashlytics
) : ViewModel() {

    private val _allDevices = MutableLiveData<List<Device>?>()

    private val _enabledDevices = MutableLiveData<List<Device>?>()
    val enabledDevices: LiveData<List<Device>?>
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
     * This function is used directly in [com.oxygenupdater.activities.OnboardingActivity],
     * whereas [enabledDevices] is used in [com.oxygenupdater.fragments.DeviceChooserOnboardingFragment]
     */
    fun fetchAllDevices(): LiveData<List<Device>?> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchDevices(DeviceRequestFilter.ALL).also { allDevices ->
            _allDevices.postValue(allDevices)
            _enabledDevices.postValue(allDevices?.filter { it.enabled })
        }
    }.let { _allDevices }

    fun fetchUpdateMethodsForDevice(deviceId: Long): LiveData<List<UpdateMethod>> = RootAccessChecker.checkRootAccess { hasRootAccess ->
        viewModelScope.launch(Dispatchers.IO) {
            serverRepository.fetchUpdateMethodsForDevice(deviceId, hasRootAccess)?.let {
                _updateMethodsForDevice.postValue(it)
            }
        }
    }.let { _updateMethodsForDevice }

    /**
     * Post [_selectedDevice] and save the ID & name in [android.content.SharedPreferences]
     */
    fun updateSelectedDevice(device: Device) {
        _selectedDevice.postValue(device)

        SettingsManager.savePreference(SettingsManager.PROPERTY_DEVICE_ID, device.id)
        SettingsManager.savePreference(SettingsManager.PROPERTY_DEVICE, device.name)
    }

    /**
     * Post [_selectedUpdateMethod] and save the ID & name in [android.content.SharedPreferences].
     * Additionally, update [FirebaseCrashlytics]' user identifier
     */
    fun updateSelectedUpdateMethod(updateMethod: UpdateMethod) {
        _selectedUpdateMethod.postValue(updateMethod)

        SettingsManager.savePreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, updateMethod.id)
        SettingsManager.savePreference(SettingsManager.PROPERTY_UPDATE_METHOD, updateMethod.name)

        // since both device and update method have been selected,
        // we can safely update Crashlytics' user identifier
        crashlytics.setUserId(
            "Device: " + SettingsManager.getPreference(SettingsManager.PROPERTY_DEVICE, "<UNKNOWN>")
                    + ", Update Method: " + SettingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "<UNKNOWN>")
        )
    }

    fun subscribeToNotificationTopics() = viewModelScope.launch(Dispatchers.IO) {
        NotificationTopicSubscriber.subscribe(
            _enabledDevices.value ?: ArrayList(),
            serverRepository.fetchAllMethods() ?: ArrayList()
        )
    }
}
