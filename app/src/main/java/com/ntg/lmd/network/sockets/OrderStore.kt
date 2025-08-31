package com.ntg.lmd.network.sockets

import com.ntg.lmd.mainscreen.data.model.Order
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class OrderStore {
    private val _state = MutableStateFlow<List<Order>>(emptyList())
    val state: StateFlow<List<Order>> get() = _state

    private val orderChannel = Channel<Order>(Channel.UNLIMITED)

    fun getChannel(): Channel<Order> = orderChannel

    fun add(order: Order) {
        val list = _state.value.toMutableList()
        if (list.none { it.id == order.id }) {
            list.add(0, order)
            _state.value = list
        }
        orderChannel.trySend(order)
    }

    fun update(updated: Order) {
        val list = _state.value.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx != -1) {
            list[idx] = updated
            _state.value = list
        }
        orderChannel.trySend(updated)
    }

    fun remove(orderId: String) {
        val list = _state.value.toMutableList()
        if (list.removeAll { it.id == orderId }) {
            _state.value = list
        }
    }
}
