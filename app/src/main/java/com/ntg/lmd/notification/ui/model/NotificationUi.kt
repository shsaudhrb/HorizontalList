package com.ntg.lmd.notification.ui.model

import com.ntg.lmd.notification.domain.model.AgentNotification

data class NotificationUi(
    val id: Long,
    val message: String,
    val timestampMs: Long, // keep the source of truth
    val type: AgentNotification.Type,
)
