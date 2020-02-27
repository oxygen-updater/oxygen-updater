package com.arjanvlek.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjanvlek.oxygenupdater.models.InstallGuidePage
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.models.ServerStatus
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.arjanvlek.oxygenupdater.activities.InstallActivity]
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class InstallViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _serverStatus = MutableLiveData<ServerStatus>()
    private val _installGuidePage = MutableLiveData<InstallGuidePage>()
    private val _logRootInstallResult = MutableLiveData<ServerPostResult>()

    fun fetchServerStatus(online: Boolean): LiveData<ServerStatus> = viewModelScope.launch(Dispatchers.IO) {
        _serverStatus.postValue(serverRepository.fetchServerStatus(online))
    }.let { _serverStatus }

    fun fetchInstallGuidePage(
        deviceId: Long,
        updateMethodId: Long,
        pageNumber: Int
    ): LiveData<InstallGuidePage> = viewModelScope.launch(Dispatchers.IO) {
        _installGuidePage.postValue(serverRepository.fetchInstallGuidePage(deviceId, updateMethodId, pageNumber))
    }.let { _installGuidePage }

    fun logRootInstall(rootInstall: RootInstall): LiveData<ServerPostResult> = viewModelScope.launch(Dispatchers.IO) {
        _logRootInstallResult.postValue(serverRepository.logRootInstall(rootInstall))
    }.let { _logRootInstallResult }
}
