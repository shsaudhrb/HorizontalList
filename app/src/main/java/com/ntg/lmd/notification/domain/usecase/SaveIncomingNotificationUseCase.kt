package com.ntg.lmd.notification.domain.usecase

import com.ntg.lmd.notification.domain.model.AgentNotification
import com.ntg.lmd.notification.domain.repository.NotificationRepository

class SaveIncomingNotificationUseCase(
    private val repo: NotificationRepository,
) {
    suspend operator fun invoke(n: AgentNotification) = repo.save(n)
}
