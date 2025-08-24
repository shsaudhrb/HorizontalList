package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
import com.ntg.lmd.mainscreen.ui.screens.orders.model.OrderUI
import com.ntg.lmd.utils.OrdersLoaderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private var page = 0
    private var endReached = false

    fun loadOrders(context: Context) {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    emptyMessage = null,
                )

            try {
                // Read JSON on IO dispatcher
                allOrders =
                    withContext(Dispatchers.IO) {
                        OrdersLoaderHelper.loadFromAssets(context)
                    }

                // reset paging
                page = 0
                endReached = false

                // first page
                val first = currentFiltered().take(PAGE_SIZE)

                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        orders = first,
                        emptyMessage = null, // ensure empty message cleared
                        errorMessage = null,
                    )

                if (first.size < PAGE_SIZE) endReached = true
            } catch (e: IOException) {
                Log.e(TAG, "Unable to read orders file.", e)
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        errorMessage = "Unable to read orders file: ${e.message ?: "IO error"}",
                    )
            } catch (e: JSONException) {
                Log.e(TAG, "Orders file format is invalid.", e)
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        errorMessage = "Orders file format is invalid: ${e.message ?: "JSON error"}",
                    )
            } catch (e: SecurityException) {
                Log.e(TAG, "Access to orders file denied.", e)
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        errorMessage = "Access to orders file denied.",
                    )
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _state.value = _state.value.copy(query = newQuery)
        page = 0
        endReached = false
        applyFilter(resetPaging = true)
    }

    private fun List<OrderUI>.sortedByNearest(): List<OrderUI> =
        sortedWith(
            compareBy<OrderUI> { it.distanceMeters == null }
                .thenBy { it.distanceMeters ?: Double.MAX_VALUE },
        )

    private fun applyFilter(resetPaging: Boolean = false) {
        val q = _state.value.query.trim()
        val filtered =
            if (q.isBlank()) {
                allOrders
            } else {
                allOrders.filter { o ->
                    o.orderNumber.contains(q, true) ||
                        o.customerName.contains(q, true) ||
                        (o.details?.contains(q, true) == true)
                }
            }

        val sorted = filtered.sortedByNearest()
        val firstPage = if (resetPaging) sorted.take(PAGE_SIZE) else _state.value.orders
        endReached = sorted.size <= PAGE_SIZE
        val emptyMsg =
            when {
                allOrders.isEmpty() && q.isBlank() -> "No active orders."
                sorted.isEmpty() && q.isNotBlank() -> "No matching orders."
                else -> null
            }

        _state.value =
            _state.value.copy(
                orders = firstPage,
                emptyMessage = emptyMsg,
                errorMessage = null,
            )
    }

    fun loadNextPage() {
        val s = state.value
        if (s.isLoading || s.isLoadingMore || endReached) return

        viewModelScope.launch {
            _state.value = s.copy(isLoadingMore = true)

            // simulate network latency
            kotlinx.coroutines.delay(LOAD_DELAY_MS)

            val base = currentFiltered()
            page += 1
            val from = page * PAGE_SIZE
            val next = if (from >= base.size) emptyList() else base.drop(from).take(PAGE_SIZE)
            if (next.isEmpty()) endReached = true

            _state.value =
                _state.value.copy(
                    isLoadingMore = false,
                    orders = _state.value.orders + next,
                )
        }
    }

    private fun currentFiltered(): List<OrderUI> {
        val q = state.value.query.trim()
        val filtered =
            if (q.isBlank()) {
                allOrders
            } else {
                allOrders.filter { o ->
                    o.orderNumber.contains(q, true) ||
                        o.customerName.contains(q, true) ||
                        (o.details?.contains(q, true) == true)
                }
            }
        return filtered.sortedWith(
            compareBy<OrderUI> { it.distanceMeters == null }
                .thenBy { it.distanceMeters ?: Double.MAX_VALUE },
        )
    }

    fun retry(context: Context) = loadOrders(context)
}
