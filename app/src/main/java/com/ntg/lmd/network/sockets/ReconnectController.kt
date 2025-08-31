package com.ntg.lmd.network.sockets

import android.os.Handler
import android.os.Looper
import android.util.Log

internal class ReconnectController(
    looper: Looper,
    private val action: () -> Unit,
) {
    private val logTag = "LMD-WS"
    private var handler: Handler? = null
    private var runnable: Runnable? = null

    init {
        handler = Handler(looper)
    }

    fun schedule(delayMs: Long) {
        cancel()
        runnable =
            Runnable {
                Log.d(logTag, "Auto-reconnecting…")
                action()
            }
        handler?.postDelayed(runnable!!, delayMs)
        Log.d(logTag, "Reconnection scheduled in ${delayMs}ms")
    }

    fun cancel() {
        runnable?.let { handler?.removeCallbacks(it) }
        runnable = null
    }
}
