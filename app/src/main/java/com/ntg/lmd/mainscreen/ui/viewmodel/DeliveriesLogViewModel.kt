package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.ui.mapper.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeliveriesLogViewModel : ViewModel() {
    private val _logs = MutableStateFlow<List<DeliveryLog>>(emptyList())
    val logs: StateFlow<List<DeliveryLog>> = _logs

    // for search
    private var allLogs: List<DeliveryLog> = emptyList()

    // load data
    fun load(context: Context) {
        viewModelScope.launch {
            // Get the use case from the Provider
            val useCase = DeliveriesLogProvider.loadDeliveryLogsUseCase(context)
            val domain = useCase()
            val ui = domain.map { it.toUi(context) }
            allLogs = ui
            _logs.value = ui
        }
    }

    // search by order ID (supports "#123" or "123")
    fun searchById(query: String) {
        val q = query.trim().removePrefix("#")
        _logs.value =
            if (q.isEmpty()) {
                allLogs
            } else {
                allLogs.filter { it.orderId.removePrefix("#").contains(q, ignoreCase = true) }
            }
    }
}
