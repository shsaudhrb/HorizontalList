package com.ntg.lmd.notification.data.repository

import com.ntg.lmd.notification.domain.model.AgentNotification
import com.ntg.lmd.notification.domain.model.NotificationFilter
import com.ntg.lmd.notification.domain.repository.NotificationRepository
import com.ntg.lmd.notification.domain.usecase.SaveIncomingNotificationUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

private const val INITIAL_ID = 1L
private const val EMPTY_ID = 0L

private const val FAKE_IO_DELAY_MS = 350L

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L

private const val MILLIS_PER_MINUTE = SECONDS_PER_MINUTE * MILLIS_PER_SECOND
private const val MILLIS_PER_HOUR = MINUTES_PER_HOUR * MILLIS_PER_MINUTE
private const val MILLIS_PER_DAY = HOURS_PER_DAY * MILLIS_PER_HOUR

private const val MINUTES_AGO_ORDER_ADDED = 9L
private const val MINUTES_AGO_WALLET = 25L
private const val DAYS_AGO_ORDER_DELIVERING = 3L

class NotificationRepositoryImpl : NotificationRepository {
    private val idGen = AtomicLong(INITIAL_ID)
    private val itemsState = MutableStateFlow<List<AgentNotification>>(emptyList())

    override fun observeAll(): Flow<List<AgentNotification>> = itemsState.asStateFlow()

    override suspend fun save(notification: AgentNotification) {
        val withId =
            if (notification.id == EMPTY_ID) {
                notification.copy(id = idGen.getAndIncrement())
            } else {
                notification
            }

        itemsState.value = (itemsState.value + withId).sortedByDescending { it.timestampMs }
    }

    override suspend fun refresh(): Result<Unit> = Result.success(Unit)

    override suspend fun loadPage(
        offset: Int,
        limit: Int,
        filter: NotificationFilter,
    ): List<AgentNotification> {
        delay(FAKE_IO_DELAY_MS)
        val base =
            when (filter) {
                NotificationFilter.All -> itemsState.value
                NotificationFilter.Orders -> itemsState.value.filter { it.type == AgentNotification.Type.ORDER_STATUS }
                NotificationFilter.Wallet -> itemsState.value.filter { it.type == AgentNotification.Type.WALLET }
                NotificationFilter.Other -> itemsState.value.filter { it.type == AgentNotification.Type.OTHER }
            }.sortedByDescending { it.timestampMs }

        if (offset >= base.size) return emptyList()
        val to = (offset + limit).coerceAtMost(base.size)
        return base.subList(offset, to)
    }
}

// Debug-only
suspend fun seedNotifications(save: SaveIncomingNotificationUseCase) {
    val now = System.currentTimeMillis()
    listOf(
        AgentNotification(
            EMPTY_ID,
            "Order No 37 Added",
            AgentNotification.Type.ORDER_STATUS,
            now - MINUTES_AGO_ORDER_ADDED * MILLIS_PER_MINUTE,
        ),
        AgentNotification(
            EMPTY_ID,
            "Transaction +200.0 EGP. Balance 6200.0",
            AgentNotification.Type.WALLET,
            now - MINUTES_AGO_WALLET * MILLIS_PER_MINUTE,
        ),
        AgentNotification(
            EMPTY_ID,
            "Order No 28 Delivering",
            AgentNotification.Type.ORDER_STATUS,
            now - DAYS_AGO_ORDER_DELIVERING * MILLIS_PER_DAY,
        ),
    ).forEach { save(it) }
}
