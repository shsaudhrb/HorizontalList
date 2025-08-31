package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
import com.ntg.lmd.mainscreen.ui.screens.orders.model.LocalUiOnlyStatusBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min

private const val PAGE_SIZE = 10
private const val LOAD_DELAY_MS = 600L // only for paging spinner feel

class MyOrdersViewModel(
    private val getMyOrders: GetMyOrdersUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MyOrdersUiState(isLoading = false))
    val state: StateFlow<MyOrdersUiState> = _state

    /** Full dataset accumulated so far (concatenated pages) */
    private val allOrders: MutableList<OrderInfo> = mutableListOf()
    /** API is 1-based */
    private var page = 1
    private var endReached = false

    // ---------- PUBLIC API ----------

    /** Initial load. Shows big loader only if there’s no data yet. */
    fun loadOrders(context: Context) {
        val alreadyHasData = _state.value.orders.isNotEmpty()
        if (_state.value.isLoading) return

        _state.update {
            it.copy(
                isLoading = !alreadyHasData,
                errorMessage = null,
                emptyMessage = null
            )
        }

        viewModelScope.launch {
            try {
                page = 1
                endReached = false

                val first = fetchPage(page, bypassCache = true)
                allOrders.clear()
                allOrders.addAll(first)
                endReached = first.size < PAGE_SIZE

                publishFilteredFirstPage()
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (!alreadyHasData) (t.message ?: "Load failed") else null
                    )
                }
                if (alreadyHasData) {
                    LocalUiOnlyStatusBus.errorEvents.tryEmit(
                        Pair(t.message ?: "Load failed") { loadOrders(context) }
                    )
                }
            }
        }
    }

    private suspend fun fetchPage(page: Int, bypassCache: Boolean = false): List<OrderInfo> {
        return getMyOrders(page = page, limit = PAGE_SIZE, bypassCache = bypassCache)
    }

    fun refresh(context: Context) {
        val s = _state.value
        if (s.isRefreshing) return
        _state.update { it.copy(isRefreshing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val fresh = fetchPage(page = 1, bypassCache = true) // <- force network
                page = 1
                endReached = fresh.size < PAGE_SIZE
                allOrders.clear()
                allOrders.addAll(fresh)
                publishFilteredFirstPage()
            } catch (t: Throwable) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(
                    (t.message ?: "Refresh failed") to { refresh(context) }
                )
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /** Infinite scroll. Error -> snackbar, keep existing list. */
    fun loadNextPage(context: Context) {
        val s = state.value
        if (s.isLoading || s.isLoadingMore || endReached) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                delay(LOAD_DELAY_MS) // optional, for spinner feel
                val nextPage = page + 1
                val next = fetchPage(nextPage)

                if (next.isEmpty() || next.size < PAGE_SIZE) endReached = true
                page = nextPage
                allOrders.addAll(next)
                publishFilteredAppend()
            } catch (t: Throwable) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(
                    Pair(t.message ?: "Couldn't load more") { loadNextPage(context) }
                )
                _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun retry(context: Context) = loadOrders(context)

    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery) }
        publishFilteredFirstPage()
    }

    /** Local-only status change (for UI). Keep if useful. */
    fun updateStatusLocally(id: String, newStatus: OrderStatus) {
        val updated = state.value.orders.map { o -> if (o.id == id) o.copy(status = newStatus) else o }
        _state.update { it.copy(orders = updated) }
    }

    // ---------- INTERNALS ----------

    private suspend fun fetchPage(page: Int): List<OrderInfo> {
        // Use case returns List<OrderInfo>; the use case should call repository/retrofit
        return getMyOrders(page = page, limit = PAGE_SIZE)
    }

    private fun currentFiltered(): List<OrderInfo> {
        val q = state.value.query.trim()
        if (q.isBlank()) return allOrders
        return allOrders.filter { o ->
            o.orderNumber.contains(q, ignoreCase = true) ||
                    o.name.contains(q, ignoreCase = true) ||
                    (o.details?.contains(q, ignoreCase = true) == true)
        }
    }

    private fun publishFilteredFirstPage() {
        val base = currentFiltered()
        val first = base.take(PAGE_SIZE)
        val emptyMsg =
            when {
                base.isEmpty() && state.value.query.isBlank() -> "No active orders."
                base.isEmpty() && state.value.query.isNotBlank() -> "No matching orders."
                else -> null
            }

        _state.update {
            it.copy(
                isLoading = false,
                isLoadingMore = false,
                orders = first,
                emptyMessage = emptyMsg,
                errorMessage = null
            )
        }
    }
    fun applyServerPatch(updated: OrderInfo) {
        // replace in visible list
        val visible = _state.value.orders.toMutableList()
        val i = visible.indexOfFirst { it.id == updated.id }
        if (i != -1) {
            visible[i] = visible[i].copy(
                status = updated.status,
                details = updated.details ?: visible[i].details
                // copy more fields if you want (name, orderNumber, etc.)
            )
            _state.update { it.copy(orders = visible) }
        }
        // replace in allOrders too (so paging/filtering remains consistent)
        val j = allOrders.indexOfFirst { it.id == updated.id }
        if (j != -1) {
            allOrders[j] = allOrders[j].copy(
                status = updated.status,
                details = updated.details ?: allOrders[j].details
            )
        }
    }

    private fun publishFilteredAppend() {
        val base = currentFiltered()
        val visibleCount = min(page * PAGE_SIZE, base.size) // page is 1-based
        _state.update { it.copy(isLoadingMore = false, orders = base.take(visibleCount)) }
    }
}
