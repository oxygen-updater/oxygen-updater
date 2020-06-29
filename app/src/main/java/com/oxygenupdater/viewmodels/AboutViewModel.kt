package com.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.oxygenupdater.activities.AboutActivity]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class AboutViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _serverStatus = MutableLiveData<ServerStatus>()

    fun fetchServerStatus(): LiveData<ServerStatus> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchServerStatus(true).let {
            _serverStatus.postValue(it)
        }
    }.let { _serverStatus }
}
