package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.model.MyOrdersPoolUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPoolViewModel(
    private val getMyOrders: GetMyOrdersUseCase,
) : ViewModel() {
    private val _ui = MutableStateFlow(MyOrdersPoolUiState())
    val ui: StateFlow<MyOrdersPoolUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() =
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true) }
            val list = getMyOrders()
            _ui.update { it.copy(isLoading = false, orders = list) }
        }

    fun onCenteredOrderChange(order: OrderInfo) {
        _ui.update { it.copy(selectedOrderNumber = order.orderNumber) }
    }
}
