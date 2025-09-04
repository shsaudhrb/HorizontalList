package com.ntg.lmd.notification.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import com.ntg.lmd.notification.data.dataSource.paging.NotificationsPagingSource
import com.ntg.lmd.notification.data.model.FCMServiceLocator
import com.ntg.lmd.notification.domain.model.AgentNotification
import com.ntg.lmd.notification.domain.model.NotificationFilter
import com.ntg.lmd.notification.domain.usecase.ObserveNotificationsUseCase
import com.ntg.lmd.notification.domain.usecase.RefreshNotificationsUseCase
import com.ntg.lmd.notification.ui.model.NotificationUi
import com.ntg.lmd.notification.ui.model.NotificationsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.Date

class NotificationsViewModel(
    private val observeNotifications: ObserveNotificationsUseCase,
    private val refreshNotifications: RefreshNotificationsUseCase,
) : ViewModel() {
    private val repo = FCMServiceLocator.notificationsRepo()

    private val _state = MutableStateFlow<NotificationsState>(NotificationsState.Loading)
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    private val _filter = MutableStateFlow(NotificationFilter.All)
    val filter: StateFlow<NotificationFilter> = _filter.asStateFlow()

    init {
        viewModelScope.launch {
            observeNotifications()
                .map { list ->
                    if (list.isEmpty()) {
                        NotificationsState.Empty
                    } else {
                        NotificationsState.Success(
                            list.map {
                                NotificationUi(
                                    id = it.id,
                                    message = it.message,
                                    timestampMs = it.timestampMs, // ← here
                                    type = it.type,
                                )
                            },
                        )
                    }
                }.catch { _state.value = NotificationsState.Error("Unable to load notifications.") }
                .collect { _state.value = it }
        }

        viewModelScope.launch {
            refreshNotifications()
                .onFailure {
                    _state.value = NotificationsState.Error("Unable to load notifications.")
                }
        }
    }

    private val repoChanges: Flow<Unit> =
        observeNotifications()
            .map { Unit }
            .onStart { emit(Unit) }

    val pagingDataFlow: Flow<PagingData<NotificationUi>> =
        combine(_filter, repoChanges) { currentFilter, _ -> currentFilter }
            .flatMapLatest { current ->
                Pager(
                    config =
                        PagingConfig(
                            pageSize = OrdersPaging.PAGE_SIZE,
                            initialLoadSize = OrdersPaging.PAGE_SIZE,
                            enablePlaceholders = false,
                        ),
                    pagingSourceFactory = {
                        NotificationsPagingSource(
                            repo = repo,
                            filter = current,
                        )
                    },
                ).flow
            }.cachedIn(viewModelScope)

    fun setFilter(newFilter: NotificationFilter) {
        if (newFilter != _filter.value) _filter.value = newFilter
    }

    fun addDummyAndRefresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            FCMServiceLocator.saveIncomingNotificationUseCase(
                AgentNotification(
                    id = 0,
                    message = "New update at ${Date(now)}",
                    type = AgentNotification.Type.OTHER,
                    timestampMs = now,
                ),
            )
        }
    }
}
