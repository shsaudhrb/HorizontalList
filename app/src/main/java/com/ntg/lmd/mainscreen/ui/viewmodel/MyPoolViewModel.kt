package com.ntg.lmd.mainscreen.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.model.MyOrdersPoolUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPoolViewModel(
    private val getMyOrders: GetMyOrdersUseCase,
) : ViewModel() {
    private val _ui = MutableStateFlow(MyOrdersPoolUiState())
    val ui: StateFlow<MyOrdersPoolUiState> = _ui

    private var page = 1
    private val pageSize = OrdersPaging.PAGE_SIZE
    private var loadingJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadingJob?.cancel()
        loadingJob =
            viewModelScope.launch {
                _ui.update { it.copy(isLoading = true, endReached = false) }
                runCatching { getMyOrders(page = 1, limit = pageSize) }
                    .onSuccess { list ->
                        page = 1
                        _ui.update { it.copy(isLoading = false, orders = list, endReached = list.size < pageSize) }
                    }.onFailure { e ->
                        _ui.update { it.copy(isLoading = false) }
                        Log.e("MyPoolVM", "Initial load failed: ${e.message}", e)
                    }
            }
    }

    /** Call this when user scrolls near the end (e.g., last 2 cards) */
    fun loadNextIfNeeded(currentIndex: Int) {
        val state = _ui.value
        if (state.isLoading || state.isLoadingMore || state.endReached) return
        if (currentIndex < state.orders.size - 2) return // not near end yet

        loadingJob?.cancel()
        loadingJob =
            viewModelScope.launch {
                _ui.update { it.copy(isLoadingMore = true) }
                val nextPage = page + 1
                runCatching { getMyOrders(page = nextPage, limit = pageSize) }
                    .onSuccess { more ->
                        val merged = mergeById(_ui.value.orders, more)
                        page = if (more.isNotEmpty()) nextPage else page
                        _ui.update {
                            it.copy(
                                isLoadingMore = false,
                                orders = merged,
                                endReached = more.size < pageSize,
                            )
                        }
                    }.onFailure { e ->
                        _ui.update { it.copy(isLoadingMore = false) }
                        Log.e("MyPoolVM", "Paging load failed: ${e.message}", e)
                    }
            }
    }

    fun onCenteredOrderChange(
        order: OrderInfo,
        index: Int = 0,
    ) {
        _ui.update { it.copy(selectedOrderNumber = order.orderNumber) }
        // Trigger paging when we center near the end
        loadNextIfNeeded(index)
    }

    /** Deduplicate on orderNumber */
    private fun mergeById(
        existing: List<OrderInfo>,
        incoming: List<OrderInfo>,
    ): List<OrderInfo> {
        if (incoming.isEmpty()) return existing
        val map = LinkedHashMap<String, OrderInfo>(existing.size + incoming.size)
        existing.forEach { map[it.orderNumber] = it }
        incoming.forEach { map[it.orderNumber] = it }
        return map.values.toList()
    }
}
