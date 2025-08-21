package com.ntg.lmd.order.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.ntg.lmd.order.domain.model.OrderStatus
import com.ntg.lmd.order.domain.model.OrderUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class OrdersViewModel : ViewModel() {
    private val _orders = MutableStateFlow(sampleOrders())
    val orders: StateFlow<List<OrderUi>> = _orders
}

private fun sampleOrders(): List<OrderUi> = listOf(
    OrderUi("811", "Abdo", 343.0, OrderStatus.DELIVERED, System.currentTimeMillis()- (39*60+10)*60_000L),
    OrderUi("806", "Hanan", 199.0, OrderStatus.FAILED, System.currentTimeMillis()- (17*60+20)*60_000L),
    OrderUi("807", "Mona", 291.0, OrderStatus.CANCELED, System.currentTimeMillis()- (12*60+20)*60_000L),
    OrderUi("813", "Mona", 291.0, OrderStatus.DELIVERED, System.currentTimeMillis()- (6*60+20)*60_000L),
    OrderUi("151515", "Hanan", 100.0, OrderStatus.CANCELED, System.currentTimeMillis()- (16*60+10)*60_000L),
)
