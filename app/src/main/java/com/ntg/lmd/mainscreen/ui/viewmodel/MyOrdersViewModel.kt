package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.model.MyOrdersUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.collections.take
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

private const val PAGE_SIZE = 10

class MyOrdersViewModel(
    private val getMyOrders: GetMyOrdersUseCase,
    private val computeDistancesUseCase: ComputeDistancesUseCase,
    initialUserId: String?,
) : ViewModel() {
    private val _state = MutableStateFlow(MyOrdersUiState(isLoading = false))
    val state: StateFlow<MyOrdersUiState> = _state.asStateFlow()

    private val currentUserId = MutableStateFlow<String?>(initialUserId)
    private val allOrders: MutableList<OrderInfo> = mutableListOf()
    private var page = 1
    private var endReached = false
    private val deviceLocation = MutableStateFlow<Location?>(null)

    init {
        refreshOrders()
    }

    fun updateDeviceLocation(location: Location?) {
        deviceLocation.value = location
        if (location != null && _state.value.orders.isNotEmpty()) {
            val computed = computeDistancesUseCase(location, _state.value.orders)
            _state.update { it.copy(orders = computed) }
        }
    }

    private fun withDistances(list: List<OrderInfo>): List<OrderInfo> {
        val loc = deviceLocation.value
        return if (loc != null) computeDistancesUseCase(loc, list) else list
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                page = 1
                endReached = false

                val uid = currentUserId.value
                val page1 =
                    getMyOrders(
                        page = 1,
                        limit = PAGE_SIZE,
                        bypassCache = true,
                        assignedAgentId = uid,
                        userOrdersOnly = true,
                    )
                allOrders.clear()
                allOrders.addAll(page1.items)
                endReached = page1.rawCount < PAGE_SIZE

                val withDist = withDistances(applyDisplayFilter(allOrders))
                _state.publishFirstPageFrom(withDist, PAGE_SIZE, state.value.query)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isLoadingMore = false, errorMessage = messageFor(e)) }
            }
        }
    }

    fun loadOrders(context: Context) {
        val alreadyHasData = _state.value.orders.isNotEmpty()
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = !alreadyHasData, errorMessage = null, emptyMessage = null) }

        viewModelScope.launch {
            try {
                page = 1
                endReached = false

                val uid = currentUserId.value
                val page1 =
                    getMyOrders(
                        page = 1,
                        limit = PAGE_SIZE,
                        bypassCache = true,
                        assignedAgentId = uid,
                        userOrdersOnly = true,
                    )
                allOrders.clear()
                allOrders.addAll(page1.items)
                endReached = page1.rawCount < PAGE_SIZE

                val withDist = withDistances(applyDisplayFilter(allOrders))
                _state.publishFirstPageFrom(withDist, PAGE_SIZE, state.value.query)
            } catch (e: Exception) {
                handleInitialLoadError(e, alreadyHasData, context, _state, ::loadOrders)
            }
        }
    }

    fun loadNextPage(context: Context) {
        val s = state.value
        if (s.isLoading || s.isLoadingMore || endReached) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                val nextPageNum = page + 1
                val uid = currentUserId.value
                val pageRes =
                    getMyOrders(
                        page = nextPageNum,
                        limit = PAGE_SIZE,
                        bypassCache = true,
                        assignedAgentId = uid,
                        userOrdersOnly = true,
                    )
                val next = pageRes.items
                endReached = pageRes.rawCount < PAGE_SIZE || next.isEmpty()
                page = nextPageNum

                allOrders.addAll(next)
                val withDist = withDistances(applyDisplayFilter(allOrders))
                _state.publishAppendFrom(withDist, page, PAGE_SIZE)
            } catch (e: Exception) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { loadNextPage(context) })
                _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun refresh(context: Context) {
        val s = _state.value
        if (s.isRefreshing) return
        _state.update { it.copy(isRefreshing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val uid = currentUserId.value
                val page1 =
                    getMyOrders(
                        page = 1,
                        limit = PAGE_SIZE,
                        bypassCache = true,
                        assignedAgentId = uid,
                        userOrdersOnly = true,
                    )
                val fresh = page1.items
                endReached = page1.rawCount < PAGE_SIZE

                allOrders.clear()
                allOrders.addAll(fresh)

                val withDist = withDistances(applyDisplayFilter(allOrders))
                _state.publishFirstPageFrom(withDist, PAGE_SIZE, state.value.query)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { refresh(context) })
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun setCurrentUserId(id: String?) {
        currentUserId.value = id
        val display = applyDisplayFilter(allOrders)
        val withDist = withDistances(display)
        _state.update { it.copy(orders = withDist) }
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
        val uid = currentUserId.value

        // 1) text query
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

    fun retry(context: Context) = loadOrders(context)

    fun updateStatusLocally(
        id: String,
        newStatus: OrderStatus,
    ) {
        val updated = state.value.orders.map { o -> if (o.id == id) o.copy(status = newStatus) else o }
        _state.update { it.copy(orders = updated) }
    }

    fun applyServerPatch(updated: OrderInfo) {
        // visible list
        val visible = _state.value.orders.toMutableList()
        val i = visible.indexOfFirst { it.id == updated.id }
        if (i != -1) {
            visible[i] =
                visible[i].copy(
                    status = updated.status,
                    details = updated.details ?: visible[i].details,
                )
            _state.update { it.copy(orders = visible) }
        }
        val j = allOrders.indexOfFirst { it.id == updated.id }
        if (j != -1) {
            allOrders[j] =
                allOrders[j].copy(
                    status = updated.status,
                    details = updated.details ?: allOrders[j].details,
                )
        }
    }

    private fun MutableStateFlow<MyOrdersUiState>.publishFirstPageFrom(
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

    private fun MutableStateFlow<MyOrdersUiState>.publishAppendFrom(
        base: List<OrderInfo>,
        page: Int,
        pageSize: Int,
    ) {
        val visibleCount = min(page * pageSize, base.size) // page is 1-based
        update { it.copy(isLoadingMore = false, orders = base.take(visibleCount)) }
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
            is HttpException -> "HTTP ${e.code()}"
            is UnknownHostException -> "No internet connection"
            is SocketTimeoutException -> "Request timed out"
            is IOException -> "Network error"
            else -> e.message ?: "Unknown error"
        }
}
