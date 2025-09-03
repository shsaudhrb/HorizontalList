package com.ntg.lmd.network.sockets

import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.ntg.lmd.mainscreen.data.model.Order
import com.ntg.lmd.mainscreen.data.model.WebSocketMessage
import com.ntg.lmd.network.authheader.SecureTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.WebSocket

private const val WS_CLOSE_NORMAL = 1000
private const val HTTP_UNAUTHORIZED = 401
private const val DEFAULT_RETRY_DELAY_MS = 3000L
private const val HEARTBEAT_INTERVAL_MS = 28_000L // every ~28s (before Phoenix’s 30s timeout)

class SocketIntegration(
    private val baseWsUrl: String,
    private val client: OkHttpClient,
    private val tokenStore: SecureTokenStore,
) {
    private val logTag = "LMD-WS"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<SocketEvent> = _events

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var hbJob: Job? = null

    @Volatile
    private var refCounter = 1

    // Orders store
    private val orderStore = OrderStore()

    fun getOrderChannel(): Channel<Order> = orderStore.getChannel()

    val orders: StateFlow<List<Order>> = orderStore.state

    private val gson = Gson()
    private val recon =
        ReconnectController(Looper.getMainLooper()) {
            // reconnect action
            currentChannelName?.let { connect(it) }
        }
    private val router = MessageRouter(gson, orderStore, _events, logTag)

    private var ws: WebSocket? = null
    private var lastAccess: String? = null
    private var currentChannelName: String? = null
    private var isDestroyed = false

    @Volatile
    private var listenerStarted = false

    private val connector =
        ConnectionController(
            baseWsUrl = baseWsUrl,
            client = client,
            tokenStore = tokenStore,
            listener =
                object : ConnectionListener {
                    override fun onOpen() {
                        _connectionState.value = ConnectionState.CONNECTED
                        startHeartbeat()
                    }

                    override fun onClosed(
                        code: Int,
                        reason: String,
                    ) {
                        stopHeartbeat()
                        _connectionState.value = ConnectionState.DISCONNECTED
                        if (code != WS_CLOSE_NORMAL) recon.schedule(DEFAULT_RETRY_DELAY_MS)
                    }

                    override fun onFailure(
                        httpCode: Int?,
                        message: String?,
                        t: Throwable?,
                    ) {
                        stopHeartbeat()
                        if (httpCode == HTTP_UNAUTHORIZED) {
                            _connectionState.value =
                                ConnectionState.ERROR("Authentication failed - token refresh needed")
                            recon.schedule(DEFAULT_RETRY_DELAY_MS)
                        } else {
                            _connectionState.value =
                                ConnectionState.ERROR(message ?: t?.message ?: "Connection failed")
                            recon.schedule(DEFAULT_RETRY_DELAY_MS)
                        }
                    }

                    override fun onMessage(text: String) {
                        router.route(text)
                    }
                },
            logTag = logTag,
        )

    fun connect(channelName: String) {
        if (isDestroyed) {
            Log.w(logTag, "connect() called after destroy")
        } else {
            val state = _connectionState.value
            val alreadyConnected =
                currentChannelName == channelName &&
                    (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING)

            when {
                alreadyConnected -> {
                    Log.d(logTag, "connect() ignored: already $state to $channelName")
                }

                tokenStore.getAccessToken().isNullOrEmpty() -> {
                    _connectionState.value = ConnectionState.ERROR("No authentication token")
                }

                else -> {
                    // Close any previous socket before opening a new one
                    ws?.close(WS_CLOSE_NORMAL, "Reconnecting to $channelName")
                    ws = null

                    lastAccess = tokenStore.getAccessToken()
                    currentChannelName = channelName
                    _connectionState.value = ConnectionState.CONNECTING
                    recon.cancel()
                    ws = connector.connect(channelName)
                }
            }
        }
    }

    fun disconnect() {
        recon.cancel()
        ws?.close(WS_CLOSE_NORMAL, "User disconnected")
        ws = null
        currentChannelName = null
        listenerStarted = false // reset so it can be started again later
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun retryConnection() {
        if (isDestroyed) {
            Log.w(logTag, "retryConnection() after destroy")
            return
        }
        recon.cancel()
        currentChannelName?.let { connect(it) }
    }

    fun startChannelListener() {
        if (listenerStarted) return
        listenerStarted = true

        scope.launch {
            for (order in orderStore.getChannel()) {
                Log.d(logTag, "ORDER FROM CHANNEL: #${order.orderNumber} - ${order.customerName}")
            }
        }
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

    fun reconnectIfTokenChanged(currentAccess: String?) {
        if (!currentAccess.isNullOrBlank() && currentAccess != lastAccess) {
            disconnect()
            connect(currentChannelName ?: "orders")
        }
    }

    private fun startHeartbeat() {
        hbJob?.cancel()
        hbJob =
            scope.launch {
                while (true) {
                    val json =
                        """{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"${refCounter++}"}"""
                    ws?.send(json)
                    delay(HEARTBEAT_INTERVAL_MS)
                }
            }
    }

    private fun stopHeartbeat() {
        hbJob?.cancel()
        hbJob = null
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
