package com.ntg.lmd.order.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.ntg.lmd.order.domain.model.OrderStatus
import com.ntg.lmd.order.domain.model.OrderUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

class OrdersViewModel : ViewModel() {
    private val _orders = MutableStateFlow(sampleOrders())
    val orders: StateFlow<List<OrderUi>> = _orders
}

private val ONE_MINUTE_MS = TimeUnit.MINUTES.toMillis(1)
private val ONE_HOUR_MS = TimeUnit.HOURS.toMillis(1)
private const val H_39 = 39
private const val H_17 = 17
private const val H_12 = 12
private const val H_06 = 6
private const val H_16 = 16

private const val M_10 = 10
private const val M_20 = 20

private const val PRICE_343 = 343.0
private const val PRICE_199 = 199.0
private const val PRICE_291 = 291.0
private const val PRICE_100 = 100.0

private const val ORDER_811 = "811"
private const val ORDER_806 = "806"
private const val ORDER_807 = "807"
private const val ORDER_813 = "813"
private const val ORDER_151515 = "151515"

private const val CUSTOMER_ABDO = "Abdo"
private const val CUSTOMER_HANAN = "Hanan"
private const val CUSTOMER_MONA = "Mona"
private fun hoursMinutesAgo(
    hours: Int,
    minutes: Int,
): Long = System.currentTimeMillis() - (hours * ONE_HOUR_MS + minutes * ONE_MINUTE_MS)


// -------- Sample data --------
private fun sampleOrders(): List<OrderUi> =
    listOf(
        OrderUi(ORDER_811, CUSTOMER_ABDO, PRICE_343, OrderStatus.DELIVERED, hoursMinutesAgo(H_39, M_10)),
        OrderUi(ORDER_806, CUSTOMER_HANAN, PRICE_199, OrderStatus.FAILED, hoursMinutesAgo(H_17, M_20)),
        OrderUi(ORDER_807, CUSTOMER_MONA, PRICE_291, OrderStatus.CANCELED, hoursMinutesAgo(H_12, M_20)),
        OrderUi(ORDER_813, CUSTOMER_MONA, PRICE_291, OrderStatus.DELIVERED, hoursMinutesAgo(H_06, M_20)),
        OrderUi(ORDER_151515, CUSTOMER_HANAN, PRICE_100, OrderStatus.CANCELED, hoursMinutesAgo(H_16, M_10)),
    )
