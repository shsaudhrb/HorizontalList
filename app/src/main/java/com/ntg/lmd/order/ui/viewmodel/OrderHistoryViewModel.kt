package com.ntg.lmd.order.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ntg.lmd.order.domain.model.OrderHistoryStatus
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import com.ntg.lmd.order.domain.model.OrdersHistoryFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderHistoryViewModel : ViewModel() {
    companion object {
        private const val PAGE_SIZE = 10
        private const val PREFETCH_THRESHOLD = 3
        private const val LOADING_DELAY_MS = 600L
    }

    private var all: List<OrderHistoryUi> = emptyList()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _filter = MutableStateFlow(OrdersHistoryFilter())
    val filter: StateFlow<OrdersHistoryFilter> = _filter

    private val _orders = MutableStateFlow<List<OrderHistoryUi>>(emptyList())
    val orders: StateFlow<List<OrderHistoryUi>> = _orders

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached

    fun loadFromAssets(
        context: Context,
        fileName: String = "OrderHistory.json",
    ) {
        viewModelScope.launch {
            val items =
                withContext(Dispatchers.IO) {
                    val json =
                        context.assets
                            .open(fileName)
                            .bufferedReader()
                            .use { it.readText() }
                    val type = object : TypeToken<List<OrderHistoryUi>>() {}.type
                    Gson().fromJson<List<OrderHistoryUi>>(json, type)
                }
            all = items
            recomputeAndResetPaging()
        }
    }

    fun setAllowedStatuses(statuses: Set<OrderHistoryStatus>) {
        _filter.value = _filter.value.copy(allowed = statuses)
        recomputeAndResetPaging()
    }

    fun setAgeAscending(ascending: Boolean) {
        _filter.value = _filter.value.copy(ageAscending = ascending)
        recomputeAndResetPaging()
    }

    fun loadMoreIfNeeded(lastVisibleIndex: Int) {
        if (_endReached.value || _isLoadingMore.value) return
        if (lastVisibleIndex >= _orders.value.size - PREFETCH_THRESHOLD) loadMore()
    }

    private fun recomputeAndResetPaging() {
        _endReached.value = false
        val filtered = currentFilteredSorted()
        _orders.value = filtered.take(PAGE_SIZE)
        if (_orders.value.size >= filtered.size) _endReached.value = true
    }

    private fun loadMore() {
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                delay(LOADING_DELAY_MS)
                val current = _orders.value
                val filtered = currentFilteredSorted()
                if (current.size >= filtered.size) {
                    _endReached.value = true
                    return@launch
                }
                val next = filtered.drop(current.size).take(PAGE_SIZE)
                _orders.value = current + next
                if (_orders.value.size >= filtered.size) _endReached.value = true
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
    fun refreshOrders() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // TODO: fetch latest from  API
                delay(600)
                all = all.shuffled()
                recomputeAndResetPaging()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun currentFilteredSorted(): List<OrderHistoryUi> {
        val f = _filter.value
        val filtered = all.filter { it.status in f.allowed }
        return if (f.ageAscending) {
            filtered.sortedBy { it.createdAtMillis }
        } else {
            filtered.sortedByDescending { it.createdAtMillis }
        }
    }
}
