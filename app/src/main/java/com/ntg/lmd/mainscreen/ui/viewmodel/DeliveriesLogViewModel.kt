package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.ui.mapper.toUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeliveriesLogViewModel : ViewModel() {
    private val _logs = MutableStateFlow<List<DeliveryLog>>(emptyList())
    val logs: StateFlow<List<DeliveryLog>> = _logs

    // for search
    private var allLogs: List<DeliveryLog> = emptyList()

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val LOAD_MORE_DELAY_MS = 250L
    }

    // paging state
    private val pageSize = DEFAULT_PAGE_SIZE
    private var nextIndex = 0
    private var currentSource: List<DeliveryLog> = emptyList()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached

    // load data
    fun load(context: Context) {
        viewModelScope.launch {
            // Get the use case from the Provider
            val useCase = DeliveriesLogProvider.loadDeliveryLogsUseCase(context)
            val domain = useCase()
            val ui = domain.map { it.toUi(context) }
            allLogs = ui
            resetPagination(allLogs)
        }
    }

    private fun resetPagination(source: List<DeliveryLog>) {
        currentSource = source
        nextIndex = 0
        _endReached.value = source.isEmpty()
        _logs.value = emptyList()
        // load first page
        loadMore()
    }

    fun loadMore() {
        if (_isLoadingMore.value || _endReached.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true

            delay(LOAD_MORE_DELAY_MS)

            val from = nextIndex
            val to = (nextIndex + pageSize).coerceAtMost(currentSource.size)

            if (from < to) {
                val nextChunk = currentSource.subList(from, to)
                _logs.value = _logs.value + nextChunk
                nextIndex = to
                _endReached.value = nextIndex >= currentSource.size
            } else {
                _endReached.value = true
            }
            _isLoadingMore.value = false
        }
    }

    // search by order ID (supports "#123" or "123")
    fun searchById(query: String) {
        val q = query.trim().removePrefix("#")
        val filtered =
            if (q.isEmpty()) {
                allLogs
            } else {
                allLogs.filter {
                    it.orderId
                        .removePrefix("#")
                        .contains(q, ignoreCase = true)
                }
            }
        resetPagination(filtered)
    }
}
