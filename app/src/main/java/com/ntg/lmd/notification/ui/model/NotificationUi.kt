package com.ntg.lmd.notification.ui.model

import com.ntg.lmd.notification.domain.model.AgentNotification

data class NotificationUi(
    val id: Long,
    val message: String,
    val timestampMs: Long,
    val type: AgentNotification.Type,
)
