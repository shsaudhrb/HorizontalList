// ==============================
// MyOrdersViewModel.kt
// ==============================
package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.screens.orders.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
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

    private val allOrders: MutableList<OrderInfo> = mutableListOf()
    private var page = 1
    private var endReached = false

    init {
        refreshOrders()
    }

    /** Simple refresh (pull first page fresh) */
    fun refreshOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                page = 1
                endReached = false
                val first = getMyOrders(page = 1, limit = PAGE_SIZE, bypassCache = true)
                allOrders.clear()
                allOrders.addAll(first)
                endReached = first.size < PAGE_SIZE
                publishFilteredFirstPage()
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, isLoadingMore = false) }
                // optional: surface error
            }
        }
    }

    /** Initial page (with banners & retry) */
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
                val first = getMyOrders(page = 1, limit = PAGE_SIZE, bypassCache = true)
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

    /** Pull-to-refresh with banner */
    fun refresh(context: Context) {
        val s = _state.value
        if (s.isRefreshing) return
        _state.update { it.copy(isRefreshing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val fresh = getMyOrders(page = 1, limit = PAGE_SIZE, bypassCache = true)
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
                val next = getMyOrders(page = nextPage, limit = PAGE_SIZE, bypassCache = true)

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

    /** Apply server patch into lists (when you want to keep an updated item visible) */
    fun applyServerPatch(updated: OrderInfo) {
        // visible list
        val visible = _state.value.orders.toMutableList()
        val i = visible.indexOfFirst { it.id == updated.id }
        if (i != -1) {
            visible[i] = visible[i].copy(
                status = updated.status,
                details = updated.details ?: visible[i].details
            )
            _state.update { it.copy(orders = visible) }
        }
        // backing list
        val j = allOrders.indexOfFirst { it.id == updated.id }
        if (j != -1) {
            allOrders[j] = allOrders[j].copy(
                status = updated.status,
                details = updated.details ?: allOrders[j].details
            )
        }
    }

    // ---------- INTERNALS ----------

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
                errorMessage = null,
                page = 1
            )
        }
    }

    private fun publishFilteredAppend() {
        val base = currentFiltered()
        val visibleCount = min(page * PAGE_SIZE, base.size) // page is 1-based
        _state.update { it.copy(isLoadingMore = false, orders = base.take(visibleCount)) }
    }
}
