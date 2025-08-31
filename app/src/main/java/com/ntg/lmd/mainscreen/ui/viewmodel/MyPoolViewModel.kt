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

const val NEAR_END_THRESHOLD = 2
private const val PREFETCH_AHEAD_PAGES = 3

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
                    .onSuccess { page1 ->
                        page = 1
                        val list = page1.items
                        val loc = deviceLocation.value
                        val computed = if (loc != null) computeDistancesUseCase(loc, list) else list
                        _ui.update {
                            it.copy(
                                isLoading = false,
                                orders = computed,
                                endReached = page1.rawCount < pageSize, // <-- raw count decides end
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
        if (currentIndex < state.orders.size - 2) return // trigger on 2nd last

        loadingJob?.cancel()
        loadingJob =
            viewModelScope.launch {
                _ui.update { it.copy(isLoadingMore = true) }

                val startPage = page + 1
                var curPage = startPage
                var lastRawCount = 0
                var appended = false
                var hops = 0

                while (hops < PREFETCH_AHEAD_PAGES) {
                    val res =
                        runCatching { getMyOrders(page = curPage, limit = pageSize) }
                            .getOrElse { e ->
                                _ui.update { it.copy(isLoadingMore = false) }
                                Log.e("MyPoolVM", "Paging load failed on p=$curPage: ${e.message}", e)
                                return@launch
                            }

                    lastRawCount = res.rawCount

                    if (res.items.isNotEmpty()) {
                        val merged = mergeById(_ui.value.orders, res.items)
                        val loc = deviceLocation.value
                        val computed = if (loc != null) computeDistancesUseCase(loc, merged) else merged

                        page = curPage
                        _ui.update {
                            it.copy(
                                isLoadingMore = false,
                                orders = computed,
                                endReached = lastRawCount < pageSize, // <-- raw decides end
                            )
                        }
                        appended = true
                        break
                    }

                    // No allowed items on this page
                    if (lastRawCount < pageSize) {
                        // Truly no more pages from server
                        page = curPage
                        _ui.update { it.copy(isLoadingMore = false, endReached = true) }
                        return@launch
                    }

                    // Try next page
                    curPage++
                    hops++
                }

                if (!appended) {
                    // We skipped ahead PREFETCH_AHEAD_PAGES but found no allowed items yet.
                    // Keep paging enabled so a further scroll can continue skipping.
                    page = curPage
                    _ui.update { it.copy(isLoadingMore = false, endReached = false) }
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
