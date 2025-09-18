package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import kotlinx.coroutines.flow.update

class OrdersListViewModel(
    private val store: OrdersStore,
    getMyOrders: GetMyOrdersUseCase,
    computeDistancesUseCase: ComputeDistancesUseCase,
) : ViewModel() {
    private val helpers = OrdersListHelpers(store, computeDistancesUseCase)

    private val controller =
        OrdersListController(
            store = store,
            helpers = helpers,
            getMyOrders = getMyOrders,
            scope = viewModelScope,
        )

    // ---------- Location ----------
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun updateDeviceLocation(location: Location?) {
        store.deviceLocation.value = location
        if (location != null &&
            store.state.value.orders
                .isNotEmpty()
        ) {
            val computed = helpers.withDistances(location, store.state.value.orders)
            store.state.update { it.copy(orders = computed) }
        }
    }

    // ---------- Public API----------
    fun setCurrentUserId(id: String?) = controller.setCurrentUserId(id)

    fun loadOrders(context: Context) = controller.loadInitial(context)

    fun retry(context: Context) = controller.loadInitial(context)

    fun refresh(context: Context) = controller.refresh(context)

    fun refreshOrders() = controller.refreshStrict()

    fun loadNextPage(context: Context) = controller.loadNextPage(context)
}
