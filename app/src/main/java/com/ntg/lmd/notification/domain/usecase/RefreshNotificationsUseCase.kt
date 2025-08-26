package com.ntg.lmd.notification.domain.usecase

import com.ntg.lmd.notification.domain.repository.NotificationRepository

class RefreshNotificationsUseCase(
    private val repo: NotificationRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repo.refresh()
}
