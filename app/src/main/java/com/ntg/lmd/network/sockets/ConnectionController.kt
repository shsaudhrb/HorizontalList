package com.ntg.lmd.network.sockets

import android.util.Log
import com.ntg.lmd.BuildConfig
import com.ntg.lmd.network.authheader.SecureTokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val WS_CLOSE_NORMAL = 1000

internal class ConnectionController(
    private val baseWsUrl: String,
    private val client: OkHttpClient,
    private val tokenStore: SecureTokenStore,
    private val listener: ConnectionListener,
    private val logTag: String = "LMD-WS",
) {
    private var currentSocket: WebSocket? = null

    fun connect(channelName: String): WebSocket {
        val access = tokenStore.getAccessToken().orEmpty()
        val anon = BuildConfig.SUPABASE_KEY
        val wsUrl = "$baseWsUrl?apikey=$anon&access_token=$access"
        Log.d(logTag, "Connecting to $wsUrl")

        // If an old socket exists, close it first
        currentSocket?.close(WS_CLOSE_NORMAL, "Reconnecting")
        currentSocket = null

        val request = Request.Builder().url(wsUrl).build()
        currentSocket =
            client.newWebSocket(
                request,
                socketListener("realtime:public:$channelName"),
            )
        return currentSocket!!
    }

    @Volatile
    private var subscribedTopic: String? = null

    private fun socketListener(topic: String) =
        object : WebSocketListener() {
            override fun onOpen(
                webSocket: WebSocket,
                response: Response,
            ) {
                listener.onOpen()

                val join = """{"topic":"$topic","event":"phx_join","payload":{},"ref":"1"}"""
                webSocket.send(join)
                Log.d(logTag, "JOIN -> $join")

                if (subscribedTopic != topic) {
                    val subscribe =
                        """
                        {"topic":"$topic","event":"postgres_changes",
                        "payload":{"event":"*","schema":"public","table":"orders"},"ref":"2"}
                        """.trimIndent()
                    webSocket.send(subscribe)
                    Log.d(logTag, "SUB -> $subscribe")
                } else {
                    Log.d(logTag, "SUB skipped (already subscribed to $topic)")
                }
            }

            override fun onMessage(
                webSocket: WebSocket,
                text: String,
            ) {
                listener.onMessage(text)
            }

            override fun onClosed(
                webSocket: WebSocket,
                code: Int,
                reason: String,
            ) {
                listener.onClosed(code, reason)
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?,
            ) {
                listener.onFailure(response?.code, response?.message, t)
            }
        }
}

internal interface ConnectionListener {
    fun onOpen()

    fun onClosed(
        code: Int,
        reason: String,
    )

    fun onFailure(
        httpCode: Int?,
        message: String?,
        t: Throwable?,
    )

    fun onMessage(text: String)
}
