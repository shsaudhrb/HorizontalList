package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.model.MyOrdersUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

class OrdersListViewModel(
    private val store: OrdersStore,
    private val getMyOrders: GetMyOrdersUseCase,
    private val computeDistancesUseCase: ComputeDistancesUseCase,
) : ViewModel() {
    private val state get() = store.state
    private val currentUserId get() = store.currentUserId
    private val deviceLocation get() = store.deviceLocation
    private val allOrders get() = store.allOrders

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun updateDeviceLocation(location: Location?) {
        deviceLocation.value = location
        if (location != null && state.value.orders.isNotEmpty()) {
            val computed = computeDistancesUseCase(location, state.value.orders)
            state.update { it.copy(orders = computed) }
        }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                store.page = 1
                store.endReached = false

                val uid = currentUserId.value
                val page1 =
                    getMyOrders(
                        page = 1,
                        limit = OrdersPaging.PAGE_SIZE,
                        bypassCache = true,
                        assignedAgentId = uid,
                        userOrdersOnly = true,
                    )

                allOrders.clear()
                allOrders.addAll(page1.items)
                store.endReached = page1.rawCount < OrdersPaging.PAGE_SIZE

                val withDist = withDistances(applyDisplayFilter(allOrders))
                state.publishFirstPageFrom(
                    withDist,
                    OrdersPaging.PAGE_SIZE,
                    state.value.query,
                    store.endReached,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = messageFor(e),
                    )
                }
            }
        }
    }

    fun loadOrders(context: Context) {
        val alreadyHasData = state.value.orders.isNotEmpty()
        if (state.value.isLoading) return

        state.update {
            it.copy(isLoading = !alreadyHasData, errorMessage = null, emptyMessage = null)
        }

        viewModelScope.launch {
            try {
                store.page = 1
                store.endReached = false

                val uid = currentUserId.value
                val page1 =
                    getMyOrders(
                        page = 1,
                        limit = OrdersPaging.PAGE_SIZE,
                        bypassCache = true,
                        assignedAgentId = uid,
                        userOrdersOnly = true,
                    )

                allOrders.clear()
                allOrders.addAll(page1.items)
                store.endReached = page1.rawCount < OrdersPaging.PAGE_SIZE

                val withDist = withDistances(applyDisplayFilter(allOrders))
                state.publishFirstPageFrom(
                    withDist,
                    OrdersPaging.PAGE_SIZE,
                    state.value.query,
                    store.endReached,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: IOException) {
                handleInitialLoadError(e, alreadyHasData, context, state, ::loadOrders)
            } catch (e: Exception) {
                handleInitialLoadError(e, alreadyHasData, context, state, ::loadOrders)
            }
        }
    }

    fun retry(context: Context) = loadOrders(context)

    fun refresh(context: Context) {
        val s = state.value
        if (s.isRefreshing) return
        state.update { it.copy(isRefreshing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val uid = currentUserId.value
                val page1 =
                    getMyOrders(
                        page = 1,
                        limit = OrdersPaging.PAGE_SIZE,
                        bypassCache = true,
                        assignedAgentId = uid,
                        userOrdersOnly = true,
                    )

                val fresh = page1.items
                store.endReached = page1.rawCount < OrdersPaging.PAGE_SIZE

                allOrders.clear()
                allOrders.addAll(fresh)

                val withDist = withDistances(applyDisplayFilter(allOrders))
                state.publishFirstPageFrom(
                    withDist,
                    OrdersPaging.PAGE_SIZE,
                    state.value.query,
                    store.endReached,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: SocketTimeoutException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { refresh(context) })
            } catch (e: Exception) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(
                    (e.message ?: "Unexpected error") to { refresh(context) },
                )
            } finally {
                state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadNextPage(context: Context) {
        val s = state.value
        if (s.isLoading || s.isLoadingMore || store.endReached) return
        viewModelScope.launch {
            state.update { it.copy(isLoadingMore = true) }
            try {
                val nextPageNum = store.page + 1
                val uid = currentUserId.value
                val pageRes =
                    getMyOrders(
                        page = nextPageNum,
                        limit = OrdersPaging.PAGE_SIZE,
                        bypassCache = true,
                        assignedAgentId = uid,
                        userOrdersOnly = true,
                    )
                val next = pageRes.items
                store.endReached = pageRes.rawCount < OrdersPaging.PAGE_SIZE || next.isEmpty()
                store.page = nextPageNum

                allOrders.addAll(next)
                val withDist = withDistances(applyDisplayFilter(allOrders))
                state.publishAppendFrom(
                    withDist,
                    store.page,
                    OrdersPaging.PAGE_SIZE,
                    store.endReached,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: UnknownHostException) {
                handlePagingError(messageFor(e), context)
            } catch (e: retrofit2.HttpException) {
                handlePagingError(messageFor(e), context)
            } catch (e: IOException) {
                handlePagingError(messageFor(e), context)
            }
        }
    }

    private fun handlePagingError(
        msg: String,
        context: Context,
    ) {
        LocalUiOnlyStatusBus.errorEvents.tryEmit(msg to { loadNextPage(context) })
        state.update { it.copy(isLoadingMore = false) }
    }

    fun setCurrentUserId(id: String?) {
        store.currentUserId.value = id
        val display = applyDisplayFilter(allOrders)
        val withDist = withDistances(display)
        state.update { it.copy(orders = withDist) }
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

    private fun applyDisplayFilter(list: List<OrderInfo>): List<OrderInfo> {
        val q = state.value.query.trim()
        val uid = store.currentUserId.value

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
        return if (uid.isNullOrBlank()) afterStatus else afterStatus.filter { it.assignedAgentId == uid }
    }

    private fun withDistances(list: List<OrderInfo>): List<OrderInfo> {
        val loc = deviceLocation.value
        return if (loc != null) computeDistancesUseCase(loc, list) else list
    }

    private fun handleInitialLoadError(
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

    private fun messageFor(e: Exception): String =
        when (e) {
            is retrofit2.HttpException -> "HTTP ${e.code()}"
            is UnknownHostException -> "No internet connection"
            is SocketTimeoutException -> "Request timed out"
            is IOException -> "Network error"
            else -> e.message ?: "Unknown error"
        }

    private fun MutableStateFlow<MyOrdersUiState>.publishFirstPageFrom(
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
        update {
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

    private fun MutableStateFlow<MyOrdersUiState>.publishAppendFrom(
        base: List<OrderInfo>,
        page: Int,
        pageSize: Int,
        endReached: Boolean,
    ) {
        val visibleCount = min(page * pageSize, base.size)
        update {
            it.copy(
                isLoadingMore = false,
                orders = base.take(visibleCount),
                endReached = endReached,
            )
        }
    }
}
