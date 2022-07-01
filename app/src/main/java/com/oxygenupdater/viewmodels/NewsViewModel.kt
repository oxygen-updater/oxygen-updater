package com.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.ServerPostResult
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.Logger.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * For [com.oxygenupdater.activities.NewsItemActivity]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class NewsViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _newsList = MutableLiveData<List<NewsItem>>()
    private val _newsItem = MutableLiveData<NewsItem?>()
    private val _markNewsItemReadResult = MutableLiveData<ServerPostResult>()

    val mayShowAds = !PrefManager.getBoolean(
        PrefManager.PROPERTY_AD_FREE,
        false
    ).also {
        logDebug(TAG, "mayShowAds: $it")
    }

    fun fetchNewsList(
        deviceId: Long,
        updateMethodId: Long
    ): LiveData<List<NewsItem>> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchNews(deviceId, updateMethodId).let {
            _newsList.postValue(it)
        }
    }.let { _newsList }

    fun fetchNewsItem(
        newsItemId: Long
    ): LiveData<NewsItem?> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchNewsItem(newsItemId).let {
            _newsItem.postValue(it)
        }
    }.let { _newsItem }

    fun toggleReadStatus(
        newsItem: NewsItem,
        newReadStatus: Boolean = !newsItem.read
    ) = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.toggleNewsItemReadStatusLocally(
            newsItem,
            newReadStatus
        )
    }

    fun markNewsItemRead(newsItem: NewsItem): LiveData<ServerPostResult> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.toggleNewsItemReadStatusLocally(newsItem, true)
        serverRepository.markNewsItemRead(newsItem.id!!)?.let {
            _markNewsItemReadResult.postValue(it)
        }
    }.let { _markNewsItemReadResult }

    companion object {
        private const val TAG = "NewsViewModel"
    }
}
