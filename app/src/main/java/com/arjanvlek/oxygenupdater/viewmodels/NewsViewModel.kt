package com.arjanvlek.oxygenupdater.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import com.arjanvlek.oxygenupdater.utils.Logger.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import org.threeten.bp.LocalDateTime

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

    private val settingsManager by inject(SettingsManager::class.java)

    val mayShowAds = !settingsManager.getPreference(
        SettingsManager.PROPERTY_AD_FREE,
        false
    )

    val mayShowInterstitialAd = LocalDateTime.parse(
        settingsManager.getPreference(
            SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN,
            "1970-01-01T00:00:00.000"
        )
    ).isBefore(LocalDateTime.now().minusMinutes(5)).let { haveFiveMinutesPassed ->
        logDebug(TAG, "mayShowInterstitialAd: mayShowAds: $mayShowAds, haveFiveMinutesPassed: $haveFiveMinutesPassed")

        mayShowAds && haveFiveMinutesPassed
    }

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

    companion object {
        private const val TAG = "NewsViewModel"
    }
}
