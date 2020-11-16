package com.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.ServerPostResult
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.Logger.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime

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

    val mayShowAds = !SettingsManager.getPreference(
        SettingsManager.PROPERTY_AD_FREE,
        false
    )

    val mayShowInterstitialAd = LocalDateTime.parse(
        SettingsManager.getPreference(
            SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN,
            "1970-01-01T00:00:00.000"
        )
    ).isBefore(LocalDateTime.now().minusMinutes(5)).let { haveFiveMinutesPassed ->
        logDebug(TAG, "mayShowInterstitialAd: mayShowAds: $mayShowAds, haveFiveMinutesPassed: $haveFiveMinutesPassed")

        mayShowAds && haveFiveMinutesPassed
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
