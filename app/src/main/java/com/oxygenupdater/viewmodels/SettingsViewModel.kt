package com.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.RootAccessChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.oxygenupdater.activities.SettingsActivity] and its child fragment
 * [com.oxygenupdater.fragments.SettingsFragment]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class SettingsViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _enabledDevices = MutableLiveData<List<Device>>()
    private val _updateMethodsForDevice = MutableLiveData<List<UpdateMethod>>()

    fun fetchEnabledDevices(): LiveData<List<Device>> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchDevices(DeviceRequestFilter.ENABLED)?.let {
            _enabledDevices.postValue(it)
        }
    }.let { _enabledDevices }

    fun fetchUpdateMethodsForDevice(
        deviceId: Long
    ): LiveData<List<UpdateMethod>> = RootAccessChecker.checkRootAccess { hasRootAccess ->
        viewModelScope.launch(Dispatchers.IO) {
            serverRepository.fetchUpdateMethodsForDevice(deviceId, hasRootAccess)?.let {
                _updateMethodsForDevice.postValue(it)
            }
        }
    }.let { _updateMethodsForDevice }
}
