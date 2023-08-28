package com.oxygenupdater.compose.ui.news

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.compose.ui.main.Screen
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

    @Suppress("DEPRECATION")
    var unreadCount = mutableIntStateOf(flow.value.count { !it.read })

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        refreshingFlow.value = true
        flow.emit(serverRepository.fetchNews())
        refreshingFlow.value = false
    }

    fun markAllRead() = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.markAllReadLocally()
        // Propagate to NewsListScreen
        flow.value.forEach { it.readState.value = true }
        Screen.NewsList.badge = null
        unreadCount.intValue = 0
    }

    fun toggleRead(
        newsItem: NewsItem,
        newRead: Boolean = !newsItem.readState.value,
    ) = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.toggleNewsItemReadLocally(newsItem, newRead)
    }.let {}
}
