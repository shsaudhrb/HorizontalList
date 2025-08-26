package com.ntg.lmd.network.sockets

import android.util.Log
import com.ntg.lmd.network.authheader.SecureTokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val WS_CLOSE_CODE_NORMAL = 1000

class SocketIntegration(
    private val baseWsUrl: String,
    private val client: OkHttpClient,
    private val tokenStore: SecureTokenStore,
) {
    private var ws: WebSocket? = null
    private var lastAccess: String? = null

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<SocketEvent> = _events

    fun connect() {
        val access = tokenStore.getAccessToken().orEmpty()
        lastAccess = access
        val anon = com.ntg.lmd.BuildConfig.SUPABASE_KEY
        val url = "$baseWsUrl?apikey=$anon&access_token=$access"
        Log.d("WS-URL", "WebSocket URL = $url")
        val req = Request.Builder().url(url).build()

        ws =
            client.newWebSocket(
                req,
                object : WebSocketListener() {
                    override fun onOpen(
                        webSocket: WebSocket,
                        response: okhttp3.Response,
                    ) {
                        Log.i("LMD-WS", "Successful WebSocket connect (code=${response.code})")
                        _events.tryEmit(SocketEvent.Open)
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {
                        Log.d("LMD-WS", "onMessage: $text")
                        _events.tryEmit(SocketEvent.Message(text))
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        Log.d("LMD-WS", "onClosed code=$code reason=$reason")
                        _events.tryEmit(SocketEvent.Closed(code, reason))
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: okhttp3.Response?,
                    ) {
                        Log.e("LMD-WS", "onFailure ${t.message}", t)
                        _events.tryEmit(SocketEvent.Error(t))
                    }
                },
            )
    }

    fun joinPhoenixChannel(
        topic: String,
        ref: String = "1",
    ) {
        val msg =
            """
            {
              "topic":"$topic",
              "event":"phx_join",
              "payload":{},
              "ref":"$ref"
            }
            """.trimIndent()
        ws?.send(msg)
        Log.d("LMD-WS", "join sent: $msg")
    }

    fun reconnectIfTokenChanged(currentAccess: String?) {
        if (currentAccess.isNullOrBlank()) return
        if (currentAccess == lastAccess) return
        close()
        connect()
    }

    fun send(text: String) {
        ws?.send(text)
    }

    fun close() {
        ws?.close(WS_CLOSE_CODE_NORMAL, "normal")
        ws = null
    }
}
