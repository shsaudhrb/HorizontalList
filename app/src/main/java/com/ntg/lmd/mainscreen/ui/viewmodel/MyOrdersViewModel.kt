package com.ntg.lmd.mainscreen.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyOrdersViewModel(
    getMyOrders: GetMyOrdersUseCase,
    computeDistancesUseCase: ComputeDistancesUseCase,
    initialUserId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(MyOrdersUiState(isLoading = false))
    private val currentUserId = MutableStateFlow<String?>(initialUserId)
    private val deviceLocation = MutableStateFlow<Location?>(null)

    // Shared store across sub-ViewModels
    private val store = OrdersStore(
        state = _state,
        currentUserId = currentUserId,
        deviceLocation = deviceLocation,
        allOrders = mutableListOf()
    )

    val listVM = OrdersListViewModel(store, getMyOrders, computeDistancesUseCase)
    val searchVM = OrdersSearchViewModel(store)
    val statusVM = OrdersStatusViewModel(store)

    val uiState: StateFlow<MyOrdersUiState> = _state.asStateFlow()

    init {
        listVM.refreshOrders()
    }

}