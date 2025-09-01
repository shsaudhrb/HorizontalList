package com.ntg.lmd.order.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import com.ntg.lmd.order.domain.model.OrderHistoryStatus
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import com.ntg.lmd.order.domain.model.OrdersHistoryFilter
import com.ntg.lmd.order.domain.model.usecase.GetOrdersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// ViewModel for managing orders history state (loading, filtering, sorting, pagination)
class OrderHistoryViewModel(
    private val getOrdersUseCase: GetOrdersUseCase,
) : ViewModel() {
    companion object {
        private const val PREFETCH_THRESHOLD = 3
    }

    private val _orders = MutableStateFlow<List<OrderHistoryUi>>(emptyList())
    val orders: StateFlow<List<OrderHistoryUi>> = _orders

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached

    private val _filter = MutableStateFlow(OrdersHistoryFilter())
    val filter: StateFlow<OrdersHistoryFilter> = _filter

    private var currentPage = 1

    fun loadOrders(token: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                currentPage = 1
                val result =
                    getOrdersUseCase(token, currentPage, OrdersPaging.PAGE_SIZE)
                        .filterByStatus(_filter.value.allowed)
                        .applySorting(_filter.value.ageAscending)

                _orders.value = result
                _endReached.value = result.isEmpty()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMoreOrders(token: String) {
        if (_isLoadingMore.value || _endReached.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                currentPage++
                val more =
                    getOrdersUseCase(token, currentPage, OrdersPaging.PAGE_SIZE)
                        .filterByStatus(_filter.value.allowed)

                if (more.isEmpty()) {
                    _endReached.value = true
                } else {
                    _orders.value = _orders.value + more
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun setAllowedStatuses(
        statuses: Set<OrderHistoryStatus>,
        token: String,
    ) {
        _filter.value = _filter.value.copy(allowed = statuses)
        loadOrders(token)
    }

    fun loadMoreIfNeeded(
        lastVisibleIndex: Int,
        token: String,
    ) {
        if (_endReached.value || _isLoadingMore.value) return
        if (lastVisibleIndex >= _orders.value.size - PREFETCH_THRESHOLD) {
            if (!_endReached.value) {
                loadMoreOrders(token)
            }
        }
    }

    fun refreshOrders(token: String) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result =
                    getOrdersUseCase(token, page = 1, limit = OrdersPaging.PAGE_SIZE)
                        .filterByStatus(_filter.value.allowed)
                        .applySorting(_filter.value.ageAscending)

                if (result.isNotEmpty()) {
                    currentPage = 1
                    _orders.value = result
                    _endReached.value = false
                } else {
                    Log.w("OrderHistoryVM", "Refresh returned empty list, keeping existing data")
                }
            } catch (e: IOException) {
                Log.e("OrderHistoryVM", "Network error: ${e.message}", e)
            } catch (e: HttpException) {
                Log.e("OrderHistoryVM", "HTTP error: ${e.message}", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun List<OrderHistoryUi>.filterByStatus(allowed: Set<OrderHistoryStatus>): List<OrderHistoryUi> =
        if (allowed.isEmpty()) {
            this
        } else {
            this.filter { it.status in allowed }
        }

    private fun List<OrderHistoryUi>.applySorting(ageAscending: Boolean): List<OrderHistoryUi> =
        if (ageAscending) {
            this.sortedBy { it.createdAtMillis }
        } else {
            this.sortedByDescending { it.createdAtMillis }
        }

    fun setAgeAscending(
        ascending: Boolean,
        token: String,
    ) {
        _filter.value = _filter.value.copy(ageAscending = ascending)
        loadOrders(token)
    }

    fun resetFilters() {
        _filter.value = OrdersHistoryFilter()
        currentPage = 1
        _endReached.value = false
    }
}
