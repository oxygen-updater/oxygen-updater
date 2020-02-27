package com.arjanvlek.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjanvlek.oxygenupdater.models.ServerStatus
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.arjanvlek.oxygenupdater.activities.AboutActivity]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class AboutViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _serverStatus = MutableLiveData<ServerStatus>()

    fun fetchServerStatus(online: Boolean): LiveData<ServerStatus> = viewModelScope.launch(Dispatchers.IO) {
        _serverStatus.postValue(serverRepository.fetchServerStatus(online))
    }.let { _serverStatus }
}
