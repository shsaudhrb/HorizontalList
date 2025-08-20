package com.ntg.lmd.network.sockets

import android.util.Log
import com.ntg.lmd.network.authheader.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val EVENT_BUFFER_CAPACITY = 64
private const val WS_CLOSE_CODE_NORMAL = 1000
private const val WS_CLOSE_REASON_NORMAL = "Normal closure"
private const val TAG_WS = "Socket"

class SocketIntegration(
    private val baseWsUrl: String,
    private val client: OkHttpClient,
    private val tokenStore: TokenStore,
) {
    private var ws: WebSocket? = null
    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = EVENT_BUFFER_CAPACITY)
    val events: SharedFlow<SocketEvent> = _events

    fun connect() {
        val url = "$baseWsUrl?token=${tokenStore.getAccessToken().orEmpty()}"
        val request = Request.Builder().url(url).build()
        ws =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(
                        webSocket: WebSocket,
                        response: okhttp3.Response,
                    ) {
                        Log.d(TAG_WS, "onOpen code=${response.code}")
                        _events.tryEmit(SocketEvent.Open)
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {
                        Log.d(TAG_WS, "onMessage len=${text.length}")
                        _events.tryEmit(SocketEvent.Message(text))
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        Log.d(TAG_WS, "onClosed code=$code reason=$reason")
                        _events.tryEmit(SocketEvent.Closed(code, reason))
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: okhttp3.Response?,
                    ) {
                        Log.e(TAG_WS, "onFailure ${t.message}", t)
                        _events.tryEmit(SocketEvent.Error(t))
                    }
                },
            )
    }

    fun send(message: String) {
        ws?.send(message)
    }

    fun close() {
        ws?.close(WS_CLOSE_CODE_NORMAL, WS_CLOSE_REASON_NORMAL)
        ws = null
    }
}
