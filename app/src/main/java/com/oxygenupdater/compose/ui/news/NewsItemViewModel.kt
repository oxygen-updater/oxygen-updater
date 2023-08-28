package com.oxygenupdater.compose.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.compose.activities.NewsItemActivity
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.Logger.logWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewsItemViewModel(private val serverRepository: ServerRepository) : ViewModel() {

    private val refreshingFlow = MutableStateFlow(true)
    private val flow = MutableStateFlow(NewsItemActivity.item)

    val state = refreshingFlow.combine(flow) { refreshing, item ->
        RefreshAwareState(refreshing, item)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RefreshAwareState(refreshingFlow.value, flow.value)
    )

    fun refreshItem(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        refreshingFlow.value = true
        flow.emit(serverRepository.fetchNewsItem(id).also { NewsItemActivity.item = it })
        refreshingFlow.value = false
    }

    fun markRead(item: NewsItem) = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.toggleNewsItemReadLocally(item, true)

        val result = serverRepository.markNewsItemRead(item.id!!) ?: return@launch
        if (!result.success) logWarning(
            TAG,
            "Failed to mark article as read on the server: ${result.errorMessage}"
        )
    }.let {}

    companion object {
        private const val TAG = "NewsItemViewModel"
    }
}
