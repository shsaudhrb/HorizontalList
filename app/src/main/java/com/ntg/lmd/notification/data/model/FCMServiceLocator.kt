package com.ntg.lmd.notification.data.model

import com.ntg.lmd.notification.data.repository.NotificationRepositoryImpl
import com.ntg.lmd.notification.domain.repository.NotificationRepository
import com.ntg.lmd.notification.domain.usecase.ObserveNotificationsUseCase
import com.ntg.lmd.notification.domain.usecase.RefreshNotificationsUseCase
import com.ntg.lmd.notification.domain.usecase.SaveIncomingNotificationUseCase

object FCMServiceLocator {
    private val repo: NotificationRepository by lazy { NotificationRepositoryImpl() }

    val observeNotificationsUseCase by lazy { ObserveNotificationsUseCase(repo) }
    val refreshNotificationsUseCase by lazy { RefreshNotificationsUseCase(repo) }
    val saveIncomingNotificationUseCase by lazy { SaveIncomingNotificationUseCase(repo) }

    fun notificationsRepo(): NotificationRepository = repo
}
