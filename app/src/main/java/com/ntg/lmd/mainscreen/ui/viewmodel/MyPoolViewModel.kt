package com.ntg.lmd.mainscreen.ui.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.model.MyOrdersPoolUiState
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolProvider.computeDistancesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPoolViewModel(
    private val getMyOrders: GetMyOrdersUseCase,
    private val computeDistancesUseCase: ComputeDistancesUseCase,
) : ViewModel() {
    private val _ui = MutableStateFlow(MyOrdersPoolUiState())
    val ui: StateFlow<MyOrdersPoolUiState> = _ui.asStateFlow()

    private val deviceLocation = MutableStateFlow<Location?>(null)
    private var page = 1
    private val pageSize = OrdersPaging.PAGE_SIZE
    private var loadingJob: Job? = null

    init {
        refresh()
    }

    fun updateDeviceLocation(location: Location?) {
        deviceLocation.value = location
        if (location != null && _ui.value.orders.isNotEmpty()) {
            val computed = computeDistancesUseCase(location, _ui.value.orders)
            _ui.update { it.copy(orders = computed, hasLocationPerm = true) }
        }
    }

    fun refresh() {
        loadingJob?.cancel()
        loadingJob =
            viewModelScope.launch {
                _ui.update { it.copy(isLoading = true, endReached = false) }
                runCatching { getMyOrders(page = 1, limit = pageSize) }
                    .onSuccess { list ->
                        page = 1
                        val loc = deviceLocation.value
                        val computed = if (loc != null) computeDistancesUseCase(loc, list) else list
                        _ui.update {
                            it.copy(
                                isLoading = false,
                                orders = computed,
                                endReached = computed.size < pageSize,
                            )
                        }
                    }.onFailure { e ->
                        _ui.update { it.copy(isLoading = false) }
                        Log.e("MyPoolVM", "Initial load failed: ${e.message}", e)
                    }
            }
    }

    fun loadNextIfNeeded(currentIndex: Int) {
        val state = _ui.value
        if (state.isLoading || state.isLoadingMore || state.endReached) return
        if (currentIndex < state.orders.size - 2) return

        loadingJob?.cancel()
        loadingJob =
            viewModelScope.launch {
                _ui.update { it.copy(isLoadingMore = true) }
                val nextPage = page + 1
                runCatching { getMyOrders(page = nextPage, limit = pageSize) }
                    .onSuccess { more ->
                        val merged = mergeById(_ui.value.orders, more)
                        val loc = deviceLocation.value
                        val computed = if (loc != null) computeDistancesUseCase(loc, merged) else merged
                        page = if (more.isNotEmpty()) nextPage else page
                        _ui.update {
                            it.copy(
                                isLoadingMore = false,
                                orders = computed,
                                endReached = more.size < pageSize,
                            )
                        }
                    }.onFailure { e ->
                        _ui.update { it.copy(isLoadingMore = false) }
                        Log.e("MyPoolVM", "Paging load failed: ${e.message}", e)
                    }
            }
    }

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

    fun onCenteredOrderChange(
        order: OrderInfo,
        index: Int = 0,
    ) {
        _ui.update { it.copy(selectedOrderNumber = order.orderNumber) }
        loadNextIfNeeded(index)
    }
}
