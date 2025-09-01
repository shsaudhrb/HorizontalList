package com.ntg.lmd.notification.data.model

import com.ntg.lmd.notification.domain.model.AgentNotification

data class NotificationPayload(
    val title: String,
    val body: String,
    val deepLink: String,
    val type: AgentNotification.Type,
)
