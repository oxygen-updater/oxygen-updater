package com.arjanvlek.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.arjanvlek.oxygenupdater.activities.NewsActivity]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class NewsViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _newsItem = MutableLiveData<NewsItem>()
    private val _markNewsItemReadResult = MutableLiveData<ServerPostResult>()

    fun fetchNewsItem(
        newsItemId: Long
    ): LiveData<NewsItem> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchNewsItem(newsItemId)?.let {
            _newsItem.postValue(it)
        }
    }.let { _newsItem }

    fun markNewsItemRead(newsItemId: Long): LiveData<ServerPostResult> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.markNewsItemRead(newsItemId)?.let {
            _markNewsItemReadResult.postValue(it)
        }
    }.let { _markNewsItemReadResult }
}
