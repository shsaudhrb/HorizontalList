package com.ntg.lmd.notification.domain.repository

import com.ntg.lmd.notification.domain.model.AgentNotification
import com.ntg.lmd.notification.domain.model.NotificationFilter
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    /** Emits notifications sorted desc by time. */
    fun observeAll(): Flow<List<AgentNotification>>

    /** Persist a notification (used by FCM handler). */
    suspend fun save(notification: AgentNotification)

    /** Best-effort refresh from remote (optional). */
    suspend fun refresh(): Result<Unit>

    suspend fun loadPage(
        offset: Int,
        limit: Int,
        filter: NotificationFilter = NotificationFilter.All,
    ): List<AgentNotification>
}
