package com.ntg.lmd.network.sockets

sealed interface SocketEvent {
    data object Open : SocketEvent

    data class Message(
        val text: String,
    ) : SocketEvent

    data class Closed(
        val code: Int,
        val reason: String,
    ) : SocketEvent

    data class Error(
        val throwable: Throwable,
    ) : SocketEvent
}
