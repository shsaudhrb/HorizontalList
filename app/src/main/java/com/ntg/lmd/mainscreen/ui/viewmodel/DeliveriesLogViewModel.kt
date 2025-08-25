package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class OrderRecord(
    val number: String,
    val customer: String,
    val total: Double,
    val status: String,
    val createdAtMillis: Long
)

enum class DeliveryState { DELIVERED, CANCELLED, FAILED, OTHER }

data class DeliveryLog(
    val orderDate: String,
    val deliveryTime: String,
    val orderId: String,
    val state: DeliveryState
)

class DeliveriesLogViewModel : ViewModel() {

    private val _logs = MutableStateFlow<List<DeliveryLog>>(emptyList())
    val logs: StateFlow<List<DeliveryLog>> = _logs

    // for search
    private var allLogs: List<DeliveryLog> = emptyList()

    // for formatting date (createdAtMillis)
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())

    fun loadFromAssets(context: Context) {
        viewModelScope.launch {
            val raw: List<OrderRecord> = try {
                val json = context.assets.open("OrderHistory.json")
                    .bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<OrderRecord>>() {}.type
                Gson().fromJson<List<OrderRecord>>(json, type) ?: emptyList()
            } catch (e: IOException) {
                Log.e("Orders", "Failed to read OrderHistory.json from assets", e)
                emptyList()
            } catch (e: JsonSyntaxException) {
                Log.e("Orders", "Invalid JSON format in OrderHistory.json", e)
                emptyList()
            }

            val mapped = raw
                .sortedByDescending { it.createdAtMillis } // newest first
                .map { it.toDeliveryLog() }

            allLogs = mapped
            _logs.value = mapped
        }
    }


    private fun OrderRecord.toDeliveryLog(): DeliveryLog {
        val state = when (status.uppercase(Locale.getDefault())) {
            "DELIVERED" -> DeliveryState.DELIVERED
            "CANCELLED" -> DeliveryState.CANCELLED
            "FAILED" -> DeliveryState.FAILED
            else -> DeliveryState.OTHER
        }
        val orderDate = dateFormatter.format(Date(createdAtMillis))
        return DeliveryLog(
            orderDate = orderDate,
            deliveryTime = formatRelativeTime(createdAtMillis),
            orderId = "#$number",
            state = state
        )
    }

    private fun formatRelativeTime(thenMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = abs(now - thenMillis)

        val minuteMs = 60_000L
        val hourMs = 60 * minuteMs
        val dayMs = 24 * hourMs

        return when {
            diff < minuteMs -> "just now"
            diff < hourMs -> "${diff / minuteMs} mins ago"
            diff < dayMs -> {
                val h = diff / hourMs
                if (h == 1L) "1 hour ago" else "$h hours ago"
            }

            else -> {
                val d = diff / dayMs
                if (d == 1L) "1 day ago" else "$d days ago"
            }
        }
    }

    // search by order ID
    fun searchById(query: String) {
        val q = query.trim().removePrefix("#")
        _logs.value =
            if (q.isEmpty()) allLogs
            else allLogs.filter { it.orderId.removePrefix("#").contains(q, ignoreCase = true) }
    }
}