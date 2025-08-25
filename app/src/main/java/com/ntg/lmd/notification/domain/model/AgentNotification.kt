package com.ntg.lmd.notification.domain.model

data class AgentNotification(
    val id: Long,
    val message: String,
    val type: Type, // ORDER_STATUS or WALLET
    val timestampMs: Long,
) {
    enum class Type { ORDER_STATUS, WALLET, OTHER }
}
