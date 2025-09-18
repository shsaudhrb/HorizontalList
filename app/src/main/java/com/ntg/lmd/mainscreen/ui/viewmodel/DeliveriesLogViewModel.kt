package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import com.ntg.lmd.mainscreen.domain.usecase.DeliveryStatusIds
import com.ntg.lmd.mainscreen.domain.usecase.GetDeliveriesLogFromApiUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeliveriesLogViewModel(
    private val getLogsUseCase: GetDeliveriesLogFromApiUseCase,
) : ViewModel() {
    private val _logs = MutableStateFlow<List<DeliveryLog>>(emptyList())
    val logs: StateFlow<List<DeliveryLog>> = _logs

    private var lastRequested: Pair<Int, String?>? = null
    private var generationCounter: Int = 0

    // paging state
    private var page = 1
    private var hasNext = true
    private var currentQuery: String? = null

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached

    private val _isRefreshing = MutableStateFlow(true)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun load() {
        reset()
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                fetchNext()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                reset()
                fetchNext()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun reset() {
        page = 1
        hasNext = true
        lastRequested = null
        generationCounter += 1
        _endReached.value = false
        _logs.value = emptyList()
    }

    fun loadMore() {
        if (_isLoadingMore.value || _endReached.value) return
        viewModelScope.launch { fetchNext() }
    }

    fun searchById(query: String) {
        currentQuery = query.trim().removePrefix("#").ifEmpty { null }
    }

    private suspend fun fetchNext() {
        if (!prepareNext()) return
        val g = generationCounter
        _isLoadingMore.value = true
        try {
            val (items, next) =
                getLogsUseCase(
                    page = page,
                    limit = OrdersPaging.PAGE_SIZE,
                    statusIds = DeliveryStatusIds.DEFAULT_LOG_STATUSES,
                    search = currentQuery,
                )
            if (g == generationCounter) applyPageResult(items, next)
        } catch (_: Throwable) {
            if (g == generationCounter) _endReached.value = true
        } finally {
            _isLoadingMore.value = false
        }
    }

    private fun prepareNext(): Boolean {
        var canProceed = true
        if (!hasNext) {
            _endReached.value = true
            canProceed = false
        }
        val key = page to currentQuery
        if (canProceed && lastRequested == key) canProceed = false
        if (canProceed) lastRequested = key
        return canProceed
    }

    private fun applyPageResult(
        items: List<DeliveryLog>,
        next: Boolean,
    ) {
        _logs.value = (_logs.value + items).distinctBy { it.orderId }
        hasNext = next
        _endReached.value = !hasNext
        if (hasNext) page += 1
    }
}
