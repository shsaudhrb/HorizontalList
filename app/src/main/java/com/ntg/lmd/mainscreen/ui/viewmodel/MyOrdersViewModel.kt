package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.take
import kotlin.math.abs
import kotlin.random.Random

private const val PAGE_SIZE = 10
private const val LOAD_DELAY_MS = 600L
private const val TAG = "MyOrdersViewModel"

class MyOrdersViewModel : ViewModel() {
    private val _state = MutableStateFlow(MyOrdersUiState(isLoading = true))
    val state: StateFlow<MyOrdersUiState> = _state

    private var allOrders: List<OrderInfo> = emptyList()
    private var allOrdersSorted: List<OrderInfo> = emptyList()

    private var page = 0
    private var endReached = false

    // One reusable comparator (nearest distance first, nulls last)
    private val byNearest =
        compareBy<OrderInfo> { it.distanceKm == null }
            .thenBy { it.distanceKm ?: Double.MAX_VALUE }

    // ---------- MOCK DATA (replace with real repo when ready) ----------
    private suspend fun loadMockOrders(): List<OrderInfo> =
        withContext(Dispatchers.Default) {
            delay(250) // simulate I/O a bit
            // seed for stable results during a single app run
            val rnd = Random(42)

            fun pickStatus(i: Int): OrderStatus =
                when (i % 8) {
                    0 -> OrderStatus.ADDED
                    1 -> OrderStatus.CONFIRMED
                    2 -> OrderStatus.CANCELED
                    3 -> OrderStatus.REASSIGNED
                    4 -> OrderStatus.PICKUP
                    5 -> OrderStatus.START_DELIVERY
                    6 -> OrderStatus.DELIVERY_FAILED
                    else -> OrderStatus.DELIVERY_DONE
                }

            // ~Riyadh center coords for mock
            val baseLat = 24.7136
            val baseLng = 46.6753

            List(25) { idx ->
                val jitterLat = (rnd.nextDouble() - 0.5) * 0.12 // ~±0.06 deg
                val jitterLng = (rnd.nextDouble() - 0.5) * 0.12
                val km = abs(rnd.nextDouble() * 18.0) // 0–18 km

                OrderInfo(
                    id = "order-${idx + 1}", // if your model is Int; change to Long if needed
                    orderNumber = "ORD-${1000 + idx}",
                    status = pickStatus(idx),
                    name = "Customer ${idx + 1}",
                    price = "---",
                    distanceKm = 0.0,
                    details = if (idx % 3 == 0) "Leave at the door." else "Call on arrival.",
                    lat = baseLat + jitterLat,
                    lng = baseLng + jitterLng,
                    customerPhone = null,
                )
            }
        }
    // ------------------------------------------------------------------

    private fun rebuildSortCache() {
        allOrdersSorted = allOrders.sortedWith(byNearest)
    }

    fun loadOrders(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, emptyMessage = null) }
            // replaced helper with mock data
            allOrders = loadMockOrders()
            rebuildSortCache()

            // reset paging
            page = 0
            endReached = false

            val first = currentFilteredFromCache().take(PAGE_SIZE)
            _state.update {
                it.copy(
                    isLoading = false,
                    orders = first,
                    emptyMessage = null,
                    errorMessage = null,
                )
            }
            if (first.size < PAGE_SIZE) endReached = true
        }
    }

    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery) }
        page = 0
        endReached = false
        applyFilter(resetPaging = true)
    }

    private fun currentFilteredFromCache(): List<OrderInfo> {
        val q = state.value.query.trim()
        val source = allOrdersSorted
        return if (q.isBlank()) {
            source
        } else {
            source.filter { o ->
                o.orderNumber.contains(q, true) ||
                    o.name.contains(q, true) ||
                    (o.details?.contains(q, true) == true)
            }
        }
    }

    private fun applyFilter(resetPaging: Boolean = false) {
        val base = currentFilteredFromCache()
        val firstPage = if (resetPaging) base.take(PAGE_SIZE) else _state.value.orders

        endReached = base.size <= PAGE_SIZE

        val emptyMsg =
            when {
                allOrders.isEmpty() && _state.value.query.isBlank() -> "No active orders."
                base.isEmpty() && _state.value.query.isNotBlank() -> "No matching orders."
                else -> null
            }

        _state.update {
            it.copy(
                orders = firstPage,
                emptyMessage = emptyMsg,
                errorMessage = null,
            )
        }
    }

    fun loadNextPage() {
        val s = state.value
        if (s.isLoading || s.isLoadingMore || endReached) return

        viewModelScope.launch {
            _state.value = s.copy(isLoadingMore = true)
            delay(LOAD_DELAY_MS) // simulate latency

            val base = currentFilteredFromCache()
            page += 1
            val from = page * PAGE_SIZE
            val next = if (from >= base.size) emptyList() else base.drop(from).take(PAGE_SIZE)

            if (next.isEmpty()) endReached = true

            _state.update {
                it.copy(
                    isLoadingMore = false,
                    orders = it.orders + next,
                )
            }
        }
    }

    fun updateStatusLocally(
        id: String,
        newStatus: OrderStatus,
    ) {
        val updated =
            state.value.orders.map { o ->
                if (o.id == id) o.copy(status = newStatus) else o
            }
        _state.update { it.copy(orders = updated) }
    }

    fun refresh(context: Context) {
        val s = _state.value
        if (s.isRefreshing || s.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                allOrders = loadMockOrders() // re-seed (or keep same list if you prefer)
                rebuildSortCache()

                val base = currentFilteredFromCache()
                page = 0
                endReached = false
                val first = base.take(PAGE_SIZE)
                if (first.size < PAGE_SIZE) endReached = true

                val emptyMsg =
                    when {
                        allOrders.isEmpty() && state.value.query.isBlank() -> "No active orders."
                        base.isEmpty() && state.value.query.isNotBlank() -> "No matching orders."
                        else -> null
                    }
                _state.update {
                    it.copy(
                        orders = first,
                        emptyMessage = emptyMsg,
                        errorMessage = null,
                    )
                }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun retry(context: Context) = loadOrders(context)
}
