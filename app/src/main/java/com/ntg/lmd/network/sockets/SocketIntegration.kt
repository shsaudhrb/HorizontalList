package com.ntg.lmd.network.sockets

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.ntg.lmd.BuildConfig
import com.ntg.lmd.mainscreen.data.model.Order
import com.ntg.lmd.mainscreen.data.model.WebSocketMessage
import com.ntg.lmd.network.authheader.SecureTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val WS_CLOSE_NORMAL = 1000
private const val HTTP_UNAUTHORIZED = 401
private const val DEFAULT_RETRY_DELAY_MS = 3000L

class SocketIntegration(
    private val baseWsUrl: String,
    private val client: OkHttpClient,
    private val tokenStore: SecureTokenStore,
) {
    private val logTag = "LMD-WS"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var ws: WebSocket? = null
    private var lastAccess: String? = null

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<SocketEvent> = _events

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val gson = Gson()
    private val orderChannel = Channel<Order>(Channel.UNLIMITED)

    fun getOrderChannel(): Channel<Order> = orderChannel

    private var currentChannelName: String? = null
    private var reconnectionHandler: Handler? = null
    private var reconnectionRunnable: Runnable? = null
    private var isDestroyed = false

    fun connect() {
        val access = tokenStore.getAccessToken().orEmpty()
        lastAccess = access
        val anon = com.ntg.lmd.BuildConfig.SUPABASE_KEY
        val url = "$baseWsUrl?apikey=$anon&access_token=$access"
        Log.d(logTag, "WebSocket URL = $url")
        val req = Request.Builder().url(url).build()

        _connectionState.value = ConnectionState.CONNECTING

        ws =
            client.newWebSocket(
                req,
                object : WebSocketListener() {
                    override fun onOpen(
                        webSocket: WebSocket,
                        response: Response,
                    ) {
                        Log.i(logTag, "Connected (code=${response.code})")
                        _events.tryEmit(SocketEvent.Open)
                        _connectionState.value = ConnectionState.CONNECTED
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {
                        Log.d(logTag, "onMessage: $text")
                        _events.tryEmit(SocketEvent.Message(text))
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        Log.d(logTag, "onClosed code=$code reason=$reason")
                        _events.tryEmit(SocketEvent.Closed(code, reason))
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?,
                    ) {
                        Log.e(logTag, "onFailure ${t.message}", t)
                        _events.tryEmit(SocketEvent.Error(t))
                        _connectionState.value =
                            ConnectionState.ERROR(t.message ?: "Connection failed")
                    }
                },
            )
    }

    fun joinPhoenixChannel(
        topic: String,
        ref: String = "1",
    ) {
        val msg = """{"topic":"$topic","event":"phx_join","payload":{},"ref":"$ref"}"""
        ws?.send(msg)
        Log.d(logTag, "join sent: $msg")
    }

    fun reconnectIfTokenChanged(currentAccess: String?) {
        if (!currentAccess.isNullOrBlank() && currentAccess != lastAccess) {
            close()
            connect()
        }
    }

    fun close() {
        ws?.close(WS_CLOSE_NORMAL, "normal")
        ws = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // ------------------- Orders (Realtime) -------------------

    fun startChannelListener() {
        scope.launch {
            for (order in orderChannel) {
                Log.d(
                    logTag,
                    "ORDER FROM CHANNEL: #${order.orderNumber} - ${order.customerName}",
                )
            }
        }
    }

    fun retryConnection() {
        if (isDestroyed) {
            Log.w(logTag, "Retry requested on destroyed manager")
            return
        }
        cancelReconnection()
        currentChannelName?.let { performConnect(it) }
    }

    private fun cancelReconnection() {
        reconnectionRunnable?.let { r -> reconnectionHandler?.removeCallbacks(r) }
        reconnectionHandler = null
        reconnectionRunnable = null
    }

    fun disconnect() {
        cancelReconnection()
        ws?.close(WS_CLOSE_NORMAL, "User disconnected")
        ws = null
        currentChannelName = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun updateOrderStatus(
        orderId: String,
        status: String,
    ) {
        val access = tokenStore.getAccessToken()
        if (access.isNullOrEmpty()) return
        val statusUpdate = mapOf("order_id" to orderId, "status" to status)
        val wsMessage = WebSocketMessage("UPDATE", statusUpdate)
        ws?.send(gson.toJson(wsMessage))
    }

    fun connect(channelName: String) {
        if (isDestroyed) {
            Log.w(logTag, "connect() on destroyed manager")
            return
        }
        currentChannelName = channelName
        performConnect(channelName)
    }

    private fun performConnect(channelName: String) {
        val access = tokenStore.getAccessToken().orEmpty()
        lastAccess = access
        val anon = BuildConfig.SUPABASE_KEY
        val wsUrl = "$baseWsUrl?apikey=$anon&access_token=$access"
        Log.d(logTag, "Connecting to $wsUrl")

        if (isDestroyed) {
            Log.w(logTag, "performConnect() on destroyed manager")
            return
        }
        if (access.isEmpty()) {
            val msg = "No authentication token"
            Log.e(logTag, msg)
            _connectionState.value = ConnectionState.ERROR(msg)
            return
        }

        _connectionState.value = ConnectionState.CONNECTING

        val request = Request.Builder().url(wsUrl).build()
        ws =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(
                        webSocket: WebSocket,
                        response: Response,
                    ) {
                        if (isDestroyed) return
                        _connectionState.value = ConnectionState.CONNECTED

                        val topic = "realtime:public:$channelName"

                        val joinMessage =
                            """{"topic":"$topic","event":"phx_join","payload":{},"ref":"1"}"""
                        webSocket.send(joinMessage)
                        Log.d(logTag, "JOIN -> $joinMessage")
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {
                        if (isDestroyed) return
                        Log.d(logTag, "MSG: $text")

                        try {
                            val root = JsonParser.parseString(text).asJsonObject
                            val event = root.get("event")?.asString ?: ""

                            when (event) {
                                "phx_reply" -> Log.d(logTag, "Channel joined")

                                "INSERT", "UPDATE", "DELETE" -> {
                                    val payload = root.getAsJsonObject("payload") ?: JsonObject()
                                    val record = payload.getAsJsonObject("record")
                                    val oldRecord = payload.getAsJsonObject("old_record")

                                    when (event) {
                                        "INSERT" -> {
                                            val order = gson.fromJson(record, Order::class.java)
                                            Log.d(
                                                logTag,
                                                """INSERT -> #${order.orderNumber} • ${order.customerName}""",
                                            )
                                            addOrder(order)
                                        }

                                        "UPDATE" -> {
                                            val order = gson.fromJson(record, Order::class.java)
                                            updateOrder(order)
                                        }

                                        "DELETE" -> {
                                            val id = oldRecord?.get("id")?.asString
                                            if (id != null) {
                                                removeOrder(id)
                                            } else {
                                                Log.d(logTag, "DELETE -> (no id in old_record)")
                                            }
                                        }
                                    }
                                }

                                "postgres_changes" -> {
                                    val payload = root.getAsJsonObject("payload")
                                    val data = payload?.getAsJsonObject("data")
                                    val type = data?.get("eventType")?.asString
                                    when (type) {
                                        "INSERT" -> {
                                            val newJson = data.getAsJsonObject("new")
                                            val order = gson.fromJson(newJson, Order::class.java)
                                            Log.d(
                                                logTag,
                                                """INSERT -> #${order.orderNumber} • ${order.customerName}""",
                                            )
                                            addOrder(order)
                                        }

                                        "UPDATE" -> {
                                            val newJson = data.getAsJsonObject("new")
                                            val order = gson.fromJson(newJson, Order::class.java)
                                            updateOrder(order)
                                        }

                                        "DELETE" -> {
                                            val oldJson = data.getAsJsonObject("old")
                                            val id = oldJson?.get("id")?.asString
                                            if (id != null) {
                                                removeOrder(id)
                                            }
                                        }
                                    }
                                }

                                "presence_state", "presence_diff", "system", "ping", "phx_close" -> Unit
                                else -> Unit
                            }
                        } catch (e: JsonSyntaxException) {
                            Log.e(logTag, "JSON syntax error: ${e.message}. Raw=$text", e)
                        } catch (e: JsonParseException) {
                            Log.e(logTag, "JSON parse error: ${e.message}. Raw=$text", e)
                        } catch (e: IllegalStateException) {
                            Log.e(logTag, "Illegal JSON state: ${e.message}. Raw=$text", e)
                        }
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        if (isDestroyed) return
                        _connectionState.value = ConnectionState.DISCONNECTED
                        if (code != WS_CLOSE_NORMAL) scheduleReconnection(DEFAULT_RETRY_DELAY_MS)
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?,
                    ) {
                        if (isDestroyed) return
                        response?.let { Log.e(logTag, "HTTP ${it.code} ${it.message}") }
                        if (response?.code == HTTP_UNAUTHORIZED) {
                            handleTokenRefresh()
                        } else {
                            _connectionState.value =
                                ConnectionState.ERROR(t.message ?: "Connection failed")
                            scheduleReconnection(DEFAULT_RETRY_DELAY_MS)
                        }
                    }
                },
            )
    }

    private fun addOrder(order: Order) {
        val list = _orders.value.toMutableList()
        if (list.none { it.id == order.id }) {
            list.add(0, order)
            _orders.value = list
        }
        orderChannel.trySend(order)
    }

    private fun updateOrder(updated: Order) {
        val list = _orders.value.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx != -1) {
            list[idx] = updated
            _orders.value = list
        }
        orderChannel.trySend(updated)
    }

    private fun removeOrder(orderId: String) {
        val list = _orders.value.toMutableList()
        if (list.removeAll { it.id == orderId }) {
            _orders.value = list
        }
    }

    private fun scheduleReconnection(delayMs: Long) {
        cancelReconnection()
        reconnectionHandler = Handler(Looper.getMainLooper())
        reconnectionRunnable =
            Runnable {
                Log.d(logTag, "Auto-reconnecting…")
                currentChannelName?.let { performConnect(it) }
            }
        reconnectionHandler?.postDelayed(reconnectionRunnable!!, delayMs)
        Log.d(logTag, "Reconnection scheduled in ${delayMs}ms")
    }

    private fun handleTokenRefresh() {
        _connectionState.value =
            ConnectionState.ERROR("Authentication failed - token refresh needed")
        scheduleReconnection(DEFAULT_RETRY_DELAY_MS)
    }
}

sealed class ConnectionState {
    data object CONNECTING : ConnectionState()

    data object CONNECTED : ConnectionState()

    data object DISCONNECTED : ConnectionState()

    data class ERROR(
        val message: String,
    ) : ConnectionState()
}
