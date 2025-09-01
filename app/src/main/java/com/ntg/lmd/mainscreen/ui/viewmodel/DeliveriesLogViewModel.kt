package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import com.ntg.lmd.mainscreen.domain.usecase.DeliveryStatusIds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeliveriesLogViewModel : ViewModel() {
    private val _logs = MutableStateFlow<List<DeliveryLog>>(emptyList())
    val logs: StateFlow<List<DeliveryLog>> = _logs

    private var lastRequested: Pair<Int, String?>? = null
    private var generationCounter: Int = 0 // increases every time we reset or refresh data

    // paging state
    private var page = 1
    private var hasNext = true
    private var currentQuery: String? = null

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun load(context: Context) {
        reset()
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                fetchNext(context)
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

    fun loadMore(context: Context) {
        if (_isLoadingMore.value || _endReached.value) return
        viewModelScope.launch { fetchNext(context) }
    }

    fun refresh(context: Context) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                reset()
                fetchNext(context)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchNext(context: Context) {
        if (!prepareNext()) return
        val g = generationCounter
        _isLoadingMore.value = true
        try {
            val useCase = DeliveriesLogProvider.getLogsUseCase(context)
            val (items, next) =
                useCase(
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
        if (canProceed && lastRequested == key) {
            canProceed = false
        }
        if (canProceed) {
            lastRequested = key
        }
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

    // search by order number
    fun searchById(query: String) {
        currentQuery = query.trim().removePrefix("#").ifEmpty { null }
    }
}
