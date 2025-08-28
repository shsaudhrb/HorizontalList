package com.ntg.lmd.network.sockets

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.ntg.lmd.mainscreen.data.model.Order
import kotlinx.coroutines.flow.MutableSharedFlow

internal class MessageRouter(
    private val gson: Gson,
    private val store: OrderStore,
    private val events: MutableSharedFlow<SocketEvent>,
    private val logTag: String,
) {
    fun route(text: String) {
        Log.d(logTag, "RAW -> $text")
        try {
            val root = JsonParser.parseString(text).asJsonObject
            val event = root.get("event")?.asString ?: ""

            when (event) {
                "phx_reply" -> {
                    Log.d(logTag, "Channel joined")
                }

                "INSERT", "UPDATE", "DELETE" -> handleClassic(root, event)
                "postgres_changes" -> handlePostgresChange(root)
                "presence_state", "presence_diff", "system", "ping", "phx_close" -> Unit
                else -> Unit
            }

            events.tryEmit(SocketEvent.Message(text))
        } catch (e: JsonSyntaxException) {
            Log.e(logTag, "JSON syntax error: ${e.message}. Raw=$text", e)
            events.tryEmit(SocketEvent.Error(e))
        } catch (e: JsonParseException) {
            Log.e(logTag, "JSON parse error: ${e.message}. Raw=$text", e)
            events.tryEmit(SocketEvent.Error(e))
        } catch (e: IllegalStateException) {
            Log.e(logTag, "Illegal JSON state: ${e.message}. Raw=$text", e)
            events.tryEmit(SocketEvent.Error(e))
        }
    }

    private fun handleClassic(
        root: JsonObject,
        event: String,
    ) {
        val payload = root.getAsJsonObject("payload") ?: JsonObject()
        val record = payload.getAsJsonObject("record")
        val oldRecord = payload.getAsJsonObject("old_record")

        when (event) {
            "INSERT" -> {
                val order = gson.fromJson(record, Order::class.java)
                Log.d(logTag, """INSERT -> #${order.orderNumber} • ${order.customerName}""")
                store.add(order)
            }

            "UPDATE" -> {
                val order = gson.fromJson(record, Order::class.java)
                store.update(order)
            }

            "DELETE" -> {
                val id = oldRecord?.get("id")?.asString
                if (id != null) store.remove(id) else Log.d(logTag, "DELETE -> no id")
            }
        }
    }

    private fun handlePostgresChange(root: JsonObject) {
        val payload = root.getAsJsonObject("payload")
        val data = payload?.getAsJsonObject("data")
        val type = data?.get("eventType")?.asString
        when (type) {
            "INSERT" -> {
                val newJson = data.getAsJsonObject("new")
                val order = gson.fromJson(newJson, Order::class.java)
                Log.d(logTag, """INSERT -> #${order.orderNumber} • ${order.customerName}""")
                store.add(order)
            }

            "UPDATE" -> {
                val newJson = data.getAsJsonObject("new")
                val order = gson.fromJson(newJson, Order::class.java)
                store.update(order)
            }

            "DELETE" -> {
                val oldJson = data.getAsJsonObject("old")
                val id = oldJson?.get("id")?.asString
                if (id != null) store.remove(id)
            }
        }
    }
}
