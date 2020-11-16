package com.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.models.InAppFaq
import com.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.oxygenupdater.activities.FaqActivity]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class FaqViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _inAppFaq = MutableLiveData<List<InAppFaq>>()
    val inAppFaq: LiveData<List<InAppFaq>>
        get() = _inAppFaq

    fun fetchFaqCategories() = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchFaq()?.let {
            _inAppFaq.postValue(it)
        }
    }
}
