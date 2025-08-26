package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
import com.ntg.lmd.mainscreen.ui.screens.orders.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.screens.orders.model.OrderUI
import com.ntg.lmd.utils.OrdersLoaderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException

private const val PAGE_SIZE = 10
private const val LOAD_DELAY_MS = 600L
private const val TAG = "MyOrdersViewModel"

class MyOrdersViewModel : ViewModel() {
    private val _state = MutableStateFlow(MyOrdersUiState(isLoading = true))
    val state: StateFlow<MyOrdersUiState> = _state

    private var allOrders: List<OrderUI> = emptyList()

    // cache a pre-sorted list (by nearest distance, nulls last)
    private var allOrdersSorted: List<OrderUI> = emptyList()

    private var page = 0
    private var endReached = false

    // One reusable comparator
    private val byNearest =
        compareBy<OrderUI> { it.distanceMeters == null }
            .thenBy { it.distanceMeters ?: Double.MAX_VALUE }

    private suspend fun readAll(context: Context): List<OrderUI> =
        // Reads all orders from assets
        withContext(Dispatchers.IO) { OrdersLoaderHelper.loadFromAssets(context) }

    private fun rebuildSortCache() { // Rebuilds the cached list sorted by nearest distance
        allOrdersSorted = allOrders.sortedWith(byNearest)
    }

    fun loadOrders(context: Context) { // loads orders, rebuilds cache, resets paging, and publishes the first page
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    emptyMessage = null,
                )
            }
            try {
                allOrders = readAll(context)
                rebuildSortCache() // sort once

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
            } catch (e: IOException) {
                Log.e(TAG, "Unable to read orders file.", e)
                val msg = "Unable to read orders file: ${e.message ?: "IO error"}"
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = msg,
                    )
                }
            } catch (e: JSONException) {
                Log.e(TAG, "Orders file format is invalid.", e)
                val msg = "Orders file format is invalid: ${e.message ?: "JSON error"}"
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = msg,
                    )
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Access to orders file denied.", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Access to orders file denied.",
                    )
                }
            }
        }
    }

    fun onQueryChange(newQuery: String) { // Updates the search query
        _state.update { it.copy(query = newQuery) }
        page = 0
        endReached = false
        applyFilter(resetPaging = true)
    }

    // returns the cached (already sorted) orders filtered by the current query
    private fun currentFilteredFromCache(): List<OrderUI> {
        val q = state.value.query.trim()
        val source = allOrdersSorted
        return if (q.isBlank()) {
            source
        } else {
            source.filter { o ->
                o.orderNumber.contains(q, true) ||
                    o.customerName.contains(q, true) ||
                    (o.details?.contains(q, true) == true)
            }
        }
    }

    // Applies the query filter to the cache and updates the visible page
    private fun applyFilter(resetPaging: Boolean = false) {
        val base = currentFilteredFromCache() // already sorted
        val firstPage =
            if (resetPaging) base.take(PAGE_SIZE) else _state.value.orders

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

    fun loadNextPage() { //  Appends the next page from the filtered cache
        val s = state.value
        if (s.isLoading || s.isLoadingMore || endReached) return

        viewModelScope.launch {
            _state.value = s.copy(isLoadingMore = true)

            // simulate network latency
            kotlinx.coroutines.delay(LOAD_DELAY_MS)

            val base = currentFilteredFromCache() // already sorted
            page += 1
            val from = page * PAGE_SIZE
            val next =
                if (from >= base.size) {
                    emptyList()
                } else {
                    base.drop(from).take(PAGE_SIZE)
                }

            if (next.isEmpty()) endReached = true

            _state.update {
                it.copy(
                    isLoadingMore = false,
                    orders = it.orders + next,
                )
            }
        }
    }

    fun updateStatusLocally( // Updates a single order’s status locally in the U
        id: Long,
        newStatus: OrderStatus,
    ) {
        val updated =
            state.value.orders.map { o ->
                if (o.id == id) {
                    o.copy(
                        status =
                            when (newStatus) {
                                OrderStatus.ADDED -> "added"
                                OrderStatus.CONFIRMED -> "confirmed"
                                OrderStatus.DISPATCHED -> "dispatched"
                                OrderStatus.DELIVERING -> "delivering"
                                OrderStatus.DELIVERED -> "delivered"
                                OrderStatus.FAILED -> "failed"
                                OrderStatus.CANCELED -> "canceled"
                            },
                    )
                } else {
                    o
                }
            }
        _state.update { it.copy(orders = updated) }
        // If distance can change dynamically in your app, call rebuildSortCache() when it does.
    }

    fun refresh(context: Context) { // Pull-to-refresh
        val s = _state.value
        if (s.isRefreshing || s.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                allOrders = readAll(context)
                rebuildSortCache() // refresh also updates cache

                // inline former applyFirstPageFrom(base)
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
            } catch (e: IOException) {
                Log.e(TAG, "Refresh IO error.", e)
                _state.update { it.copy(errorMessage = e.message ?: "Refresh failed (IO)") }
            } catch (e: JSONException) {
                Log.e(TAG, "Refresh JSON parse error.", e)
                _state.update { it.copy(errorMessage = e.message ?: "Refresh failed (JSON)") }
            } catch (e: SecurityException) {
                Log.e(TAG, "Refresh security error.", e)
                _state.update { it.copy(errorMessage = "Access denied during refresh") }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun retry(context: Context) = loadOrders(context)
}
