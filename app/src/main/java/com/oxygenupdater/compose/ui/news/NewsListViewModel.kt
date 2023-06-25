package com.oxygenupdater.compose.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.repositories.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NewsListViewModel(private val serverRepository: ServerRepository) : ViewModel() {

    private val refreshingFlow = MutableStateFlow(true)
    private val flow = MutableStateFlow(try {
        runBlocking(Dispatchers.IO) { serverRepository.fetchNewsFromDb() }
    } catch (e: InterruptedException) {
        listOf()
    })

    val state = refreshingFlow.combine(flow) { refreshing, list ->
        RefreshAwareState(refreshing, list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RefreshAwareState(refreshingFlow.value, flow.value)
    )

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        val deviceId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        refreshingFlow.emit(true)
        flow.emit(serverRepository.fetchNews(deviceId, updateMethodId))
        refreshingFlow.emit(false)
    }

    fun markAllRead() = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.markAllReadLocally()
    }

    fun toggleRead(
        newsItem: NewsItem,
        newRead: Boolean = !newsItem.read,
    ) = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.toggleNewsItemReadLocally(newsItem, newRead)
    }
}
