package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import android.location.Location
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.model.MyOrdersUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.collections.contains

fun MutableStateFlow<MyOrdersUiState>.publishFirstPageFrom(
    base: List<OrderInfo>,
    pageSize: Int,
    query: String,
) {
    val first = base.take(pageSize)
    val emptyMsg =
        when {
            base.isEmpty() && query.isBlank() -> "No active orders."
            base.isEmpty() && query.isNotBlank() -> "No matching orders."
            else -> null
        }
    update {
        it.copy(
            isLoading = false,
            isLoadingMore = false,
            orders = first,
            emptyMessage = emptyMsg,
            errorMessage = null,
            page = 1,
        )
    }
}

fun MutableStateFlow<MyOrdersUiState>.publishAppendFrom(
    base: List<OrderInfo>,
    page: Int,
    pageSize: Int,
) {
    val visibleCount = kotlin.math.min(page * pageSize, base.size)
    update { it.copy(isLoadingMore = false, orders = base.take(visibleCount)) }
}

fun handleInitialLoadError(
    e: Throwable,
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

fun messageFor(e: Throwable): String =
    when (e) {
        is HttpException -> "HTTP ${e.code()}"
        is UnknownHostException -> "No internet connection"
        is SocketTimeoutException -> "Request timed out"
        is IOException -> "Network error"
        else -> e.message ?: "Unknown error"
    }

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

data class PageBundle(
    val combined: List<OrderInfo>,
    val endReached: Boolean,
    val page: Int,
)

class OrdersPaginator(
    private val getMyOrders: GetMyOrdersUseCase,
    private val pageSize: Int,
) {
    private val allOrders = mutableListOf<OrderInfo>()
    private var page: Int = 1
    private var endReached: Boolean = false

    suspend fun loadFirst(uid: String?): PageBundle {
        page = 1
        endReached = false
        val res =
            getMyOrders(
                page = 1,
                limit = pageSize,
                bypassCache = true,
                assignedAgentId = uid,
                userOrdersOnly = true,
            )
        allOrders.clear()
        allOrders.addAll(res.items)
        endReached = res.rawCount < pageSize
        return PageBundle(allOrders.toList(), endReached, page)
    }

    suspend fun loadNext(uid: String?): PageBundle? {
        if (endReached) {
            return null
        }

        val nextPage = page + 1
        val res =
            getMyOrders(
                page = nextPage,
                limit = pageSize,
                bypassCache = true,
                assignedAgentId = uid,
                userOrdersOnly = true,
            )

        val next = res.items
        endReached = res.rawCount < pageSize || next.isEmpty()

        return if (next.isEmpty()) {
            null
        } else {
            allOrders.addAll(next)
            page = nextPage
            PageBundle(allOrders.toList(), endReached, page)
        }
    }

    fun snapshot(): List<OrderInfo> = allOrders.toList()

    fun applyServerPatch(updated: OrderInfo) {
        val j = allOrders.indexOfFirst { it.id == updated.id }
        if (j != -1) {
            allOrders[j] =
                allOrders[j].copy(
                    status = updated.status,
                    details = updated.details ?: allOrders[j].details,
                )
        }
    }
}

fun withDistances(
    list: List<OrderInfo>,
    location: Location?,
    computeDistancesUseCase: ComputeDistancesUseCase,
): List<OrderInfo> = if (location != null) computeDistancesUseCase(location, list) else list
