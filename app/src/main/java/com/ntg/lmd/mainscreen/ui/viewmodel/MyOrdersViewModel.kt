package com.ntg.lmd.mainscreen.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
import com.ntg.lmd.mainscreen.ui.screens.orders.model.OrderUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyOrdersViewModel : ViewModel() {
    private val _state = MutableStateFlow(MyOrdersUiState(isLoading = true))
    val state: StateFlow<MyOrdersUiState> = _state

    private var allOrders: List<OrderUI> = emptyList()

    fun loadOrders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, emptyMessage = null)
            try {
                val mockOrders =
                    listOf(
                        OrderUI(1, "181818", "confirmed", "Hanan", 100.0, "new", 1700.0),
                        OrderUI(2, "202020", "added", "Hanan", 400.0, "new", 2000.0),
                        OrderUI(3, "648383", "confirmed", "Hanan", 100.0, "new", 1300.0),
                        OrderUI(4, "000999", "added", "Hanan", 400.0, "new", 3000.0),
                    )
                allOrders = mockOrders
                // ↓ flip loading off before filtering so UI can update
                _state.value = _state.value.copy(isLoading = false)
                applyFilter()
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        errorMessage = "Unable to load orders",
                    )
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _state.value = _state.value.copy(query = newQuery)
        applyFilter()
    }

    private fun List<OrderUI>.sortedByNearest(): List<OrderUI> =
        this.sortedWith(
            compareBy<OrderUI> { it.distanceMeters == null } // false (has distance) first
                .thenBy { it.distanceMeters ?: Double.MAX_VALUE }, // then shortest distance
        )

    private fun applyFilter() {
        val q = _state.value.query.trim()

        val filtered =
            if (q.isBlank()) {
                allOrders
            } else {
                allOrders.filter { o ->
                    o.orderNumber.contains(q, ignoreCase = true) ||
                        o.customerName.contains(q, ignoreCase = true) ||
                        (o.details?.contains(q, ignoreCase = true) == true)
                }
            }
        val sorted = filtered.sortedByNearest()

        val emptyMsg =
            when {
                allOrders.isEmpty() && q.isBlank() -> "No active orders."
                filtered.isEmpty() && q.isNotBlank() -> "No matching orders."
                else -> null
            }

        _state.value =
            _state.value.copy(
                orders = sorted,
                emptyMessage = emptyMsg,
                errorMessage = null,
            )
    }

    fun retry() = loadOrders()
}
