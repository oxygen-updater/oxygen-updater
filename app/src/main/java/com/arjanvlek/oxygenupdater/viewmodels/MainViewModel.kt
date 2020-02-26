package com.arjanvlek.oxygenupdater.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.models.ServerMessage
import com.arjanvlek.oxygenupdater.models.ServerStatus
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Shared between [com.arjanvlek.oxygenupdater.activities.MainActivity] and its three child fragments
 * (as part of [androidx.viewpager.widget.ViewPager]):
 * 1. [com.arjanvlek.oxygenupdater.fragments.NewsFragment]
 * 2. [com.arjanvlek.oxygenupdater.fragments.UpdateInformationFragment]
 * 3. [com.arjanvlek.oxygenupdater.fragments.DeviceInformationFragment]
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class MainViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _allDevices = MutableLiveData<List<Device>>()
    val allDevices: LiveData<List<Device>>
        get() = _allDevices

    private val _updateData = MutableLiveData<UpdateData>()
    private val _newsList = MutableLiveData<List<NewsItem>>()
    private val _serverStatus = MutableLiveData<ServerStatus>()
    private val _serverMessages = MutableLiveData<List<ServerMessage>>()

    fun fetchAllDevices(): LiveData<List<Device>> = viewModelScope.launch(Dispatchers.IO) {
        _allDevices.postValue(serverRepository.getDevices(DeviceRequestFilter.ALL, false))
    }.let { _allDevices }

    fun fetchUpdateData(
        online: Boolean,
        deviceId: Long,
        updateMethodId: Long,
        incrementalSystemVersion: String,
        errorCallback: KotlinCallback<String?>
    ): LiveData<UpdateData> = viewModelScope.launch(Dispatchers.IO) {
        _updateData.postValue(serverRepository.getUpdateData(online, deviceId, updateMethodId, incrementalSystemVersion, errorCallback))
    }.let { _updateData }

    fun fetchNews(
        context: Context,
        deviceId: Long,
        updateMethodId: Long
    ): LiveData<List<NewsItem>> = viewModelScope.launch(Dispatchers.IO) {
        _newsList.postValue(serverRepository.getNews(context, deviceId, updateMethodId))
    }.let { _newsList }

    fun fetchServerStatus(online: Boolean): LiveData<ServerStatus> = viewModelScope.launch(Dispatchers.IO) {
        _serverStatus.postValue(serverRepository.getServerStatus(online))
    }.let { _serverStatus }

    fun fetchServerMessages(
        serverStatus: ServerStatus,
        errorCallback: KotlinCallback<String?>
    ): LiveData<List<ServerMessage>> = viewModelScope.launch(Dispatchers.IO) {
        _serverMessages.postValue(serverRepository.getServerMessages(serverStatus, errorCallback))
    }.let { _serverMessages }
}
