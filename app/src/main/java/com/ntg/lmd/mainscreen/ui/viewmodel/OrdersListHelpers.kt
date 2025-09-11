package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import android.location.Location
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.model.MyOrdersUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.min

@Suppress("UnusedPrivateProperty")
class OrdersListHelpers(
    private val store: OrdersStore,
    private val computeDistancesUseCase: ComputeDistancesUseCase,
) {
    private val allowedStatuses =
        setOf(
            OrderStatus.ADDED,
            OrderStatus.CONFIRMED,
            OrderStatus.REASSIGNED,
            OrderStatus.CANCELED,
            OrderStatus.PICKUP,
            OrderStatus.START_DELIVERY,
        )

    fun applyDisplayFilter(
        list: List<OrderInfo>,
        query: String,
        currentUserId: String?,
    ): List<OrderInfo> {
        val q = query.trim()
        val afterQuery =
            if (q.isBlank()) {
                list
            } else {
                list.filter { o ->
                    o.orderNumber.contains(q, ignoreCase = true) ||
                            o.name.contains(q, ignoreCase = true) ||
                            (o.details?.contains(q, ignoreCase = true) == true)
                }
            }
        val afterStatus = afterQuery.filter { it.status in allowedStatuses }
        return if (currentUserId.isNullOrBlank()) {
            afterStatus
        } else {
            afterStatus.filter { it.assignedAgentId == currentUserId }
        }
    }

    fun computeDisplay(
        location: Location?,
        source: List<OrderInfo>,
        query: String?,
        uid: String?,
    ): List<OrderInfo> {
        val filtered = applyDisplayFilter(source, query.orEmpty(), uid)
        return withDistances(location, filtered)
    }

    fun withDistances(
        location: Location?,
        list: List<OrderInfo>,
    ): List<OrderInfo> = if (location != null) computeDistancesUseCase(location, list) else list

    fun handlePagingError(
        msg: String,
        context: Context,
        state: MutableStateFlow<MyOrdersUiState>,
        retry: (Context) -> Unit,
    ) {
        LocalUiOnlyStatusBus.errorEvents.tryEmit(msg to { retry(context) })
        state.update { it.copy(isLoadingMore = false) }
    }

    fun handleInitialLoadError(
        e: Exception,
        alreadyHasData: Boolean,
        context: Context,
        state: MutableStateFlow<MyOrdersUiState>,
        retry: (Context) -> Unit,
    ) {
        val msg = messageFor(e)
        state.update {
            it.copy(
                isLoading = false,
                errorMessage = if (!alreadyHasData) msg else null,
            )
        }
        if (alreadyHasData) {
            LocalUiOnlyStatusBus.errorEvents.tryEmit(msg to { retry(context) })
        }
    }

    fun messageFor(e: Exception): String =
        when (e) {
            is retrofit2.HttpException -> "HTTP ${e.code()}"
            is UnknownHostException -> "No internet connection"
            is SocketTimeoutException -> "Request timed out"
            is IOException -> "Network error"
            else -> e.message ?: "Unknown error"
        }

    fun publishFirstPageFrom(
        state: MutableStateFlow<MyOrdersUiState>,
        base: List<OrderInfo>,
        pageSize: Int,
        query: String,
        endReached: Boolean,
    ) {
        val first = base.take(pageSize)
        val emptyMsg =
            when {
                base.isEmpty() && query.isBlank() -> "No active orders."
                base.isEmpty() && query.isNotBlank() -> "No matching orders."
                else -> null
            }
        state.update {
            it.copy(
                isLoading = false,
                isLoadingMore = false,
                orders = first,
                emptyMessage = emptyMsg,
                errorMessage = null,
                page = 1,
                endReached = endReached,
            )
        }
    }

    fun publishAppendFrom(
        state: MutableStateFlow<MyOrdersUiState>,
        base: List<OrderInfo>,
        page: Int,
        pageSize: Int,
        endReached: Boolean,
    ) {
        val visibleCount = min(page * pageSize, base.size)
        state.update {
            it.copy(
                isLoadingMore = false,
                orders = base.take(visibleCount),
                endReached = endReached,
            )
        }
    }
}
