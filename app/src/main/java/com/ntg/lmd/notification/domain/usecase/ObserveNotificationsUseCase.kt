package com.ntg.lmd.notification.domain.usecase

import com.ntg.lmd.notification.domain.model.AgentNotification
import com.ntg.lmd.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

class ObserveNotificationsUseCase(
    private val repo: NotificationRepository,
) {
    operator fun invoke(): Flow<List<AgentNotification>> = repo.observeAll()
}
