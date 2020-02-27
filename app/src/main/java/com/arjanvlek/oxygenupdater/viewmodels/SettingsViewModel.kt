package com.arjanvlek.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjanvlek.oxygenupdater.enums.PurchaseType
import com.arjanvlek.oxygenupdater.internal.iab.Purchase
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.models.UpdateMethod
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import com.arjanvlek.oxygenupdater.utils.RootAccessChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.arjanvlek.oxygenupdater.activities.SettingsActivity]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class SettingsViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _enabledDevices = MutableLiveData<List<Device>>()
    private val _updateMethodsForDevice = MutableLiveData<List<UpdateMethod>>()
    private val _verifyPurchaseResult = MutableLiveData<ServerPostResult>()

    fun fetchEnabledDevices(): LiveData<List<Device>> = viewModelScope.launch(Dispatchers.IO) {
        _enabledDevices.postValue(serverRepository.fetchDevices(DeviceRequestFilter.ENABLED, true))
    }.let { _enabledDevices }

    fun fetchUpdateMethodsForDevice(
        deviceId: Long
    ): LiveData<List<UpdateMethod>> = RootAccessChecker.checkRootAccess { hasRootAccess ->
        viewModelScope.launch(Dispatchers.IO) {
            _updateMethodsForDevice.postValue(serverRepository.fetchUpdateMethodsForDevice(deviceId, hasRootAccess))
        }
    }.let { _updateMethodsForDevice }

    fun verifyPurchase(
        purchase: Purchase,
        amount: String?,
        purchaseType: PurchaseType
    ): LiveData<ServerPostResult> = viewModelScope.launch(Dispatchers.IO) {
        _verifyPurchaseResult.postValue(serverRepository.verifyPurchase(purchase, amount, purchaseType))
    }.let { _verifyPurchaseResult }
}
