package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.model.MyOrdersUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.screens.orders.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.take
import kotlin.random.Random
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

private const val PAGE_SIZE = 10

class MyOrdersViewModel(
    private val getMyOrders: GetMyOrdersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MyOrdersUiState(isLoading = false))
    val state: StateFlow<MyOrdersUiState> = _state

    private val allOrders: MutableList<OrderInfo> = mutableListOf()
    private var page = 1
    private var endReached = false

    init { refreshOrders() }

    /** Simple refresh (pull first page fresh) */
    fun refreshOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                page = 1
                endReached = false
                val first = getMyOrders(page = 1, limit = PAGE_SIZE, bypassCache = true)
                allOrders.clear(); allOrders.addAll(first)
                endReached = first.size < PAGE_SIZE
                val base = currentFilteredFor(state.value.query, allOrders)
                _state.publishFirstPageFrom(base, PAGE_SIZE, state.value.query)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: HttpException) {
                _state.update { it.copy(isLoading = false, isLoadingMore = false, errorMessage = messageFor(e)) }
            } catch (e: UnknownHostException) {
                _state.update { it.copy(isLoading = false, isLoadingMore = false, errorMessage = messageFor(e)) }
            } catch (e: SocketTimeoutException) {
                _state.update { it.copy(isLoading = false, isLoadingMore = false, errorMessage = messageFor(e)) }
            } catch (e: IOException) {
                _state.update { it.copy(isLoading = false, isLoadingMore = false, errorMessage = messageFor(e)) }
            }
        }
    }

    /** Initial page (with banners & retry) */
    fun loadOrders(context: Context) {
        val alreadyHasData = _state.value.orders.isNotEmpty()
        if (_state.value.isLoading) return

        _state.update { it.copy(isLoading = !alreadyHasData, errorMessage = null, emptyMessage = null) }

        viewModelScope.launch {
            try {
                page = 1
                endReached = false
                val first = getMyOrders(page = 1, limit = PAGE_SIZE, bypassCache = true)
                allOrders.clear(); allOrders.addAll(first)
                endReached = first.size < PAGE_SIZE
                val base = currentFilteredFor(state.value.query, allOrders)
                _state.publishFirstPageFrom(base, PAGE_SIZE, state.value.query)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: HttpException) {
                handleInitialLoadError(e, alreadyHasData, context, _state, ::loadOrders)
            } catch (e: UnknownHostException) {
                handleInitialLoadError(e, alreadyHasData, context, _state, ::loadOrders)
            } catch (e: SocketTimeoutException) {
                handleInitialLoadError(e, alreadyHasData, context, _state, ::loadOrders)
            } catch (e: IOException) {
                handleInitialLoadError(e, alreadyHasData, context, _state, ::loadOrders)
            }
        }
    }

    /** Pull-to-refresh with banner */
    fun refresh(context: Context) {
        val s = _state.value
        if (s.isRefreshing) return
        _state.update { it.copy(isRefreshing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val fresh = getMyOrders(page = 1, limit = PAGE_SIZE, bypassCache = true)
                page = 1
                endReached = fresh.size < PAGE_SIZE
                allOrders.clear(); allOrders.addAll(fresh)
                val base = currentFilteredFor(state.value.query, allOrders)
                _state.publishFirstPageFrom(base, PAGE_SIZE, state.value.query)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: HttpException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { refresh(context) })
            } catch (e: UnknownHostException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { refresh(context) })
            } catch (e: SocketTimeoutException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { refresh(context) })
            } catch (e: IOException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { refresh(context) })
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /** Infinite scroll. Error -> snackbar, keep existing list. */
    fun loadNextPage(context: Context) {
        val s = state.value
        if (s.isLoading || s.isLoadingMore || endReached) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                val nextPage = page + 1
                val next = getMyOrders(page = nextPage, limit = PAGE_SIZE, bypassCache = true)
                if (next.isEmpty() || next.size < PAGE_SIZE) endReached = true
                page = nextPage
                allOrders.addAll(next)

                val base = currentFilteredFor(state.value.query, allOrders)
                _state.publishAppendFrom(base, page, PAGE_SIZE)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: HttpException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { loadNextPage(context) })
                _state.update { it.copy(isLoadingMore = false) }
            } catch (e: UnknownHostException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { loadNextPage(context) })
                _state.update { it.copy(isLoadingMore = false) }
            } catch (e: SocketTimeoutException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { loadNextPage(context) })
                _state.update { it.copy(isLoadingMore = false) }
            } catch (e: IOException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(messageFor(e) to { loadNextPage(context) })
                _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun retry(context: Context) = loadOrders(context)

    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery) }
        val base = currentFilteredFor(newQuery, allOrders)
        _state.publishFirstPageFrom(base, PAGE_SIZE, newQuery)
    }

    /** Local-only status change (for UI). Keep if useful. */
    fun updateStatusLocally(id: String, newStatus: OrderStatus) {
        val updated = state.value.orders.map { o -> if (o.id == id) o.copy(status = newStatus) else o }
        _state.update { it.copy(orders = updated) }
    }

    /** Apply server patch into lists (when you want to keep an updated item visible) */
    fun applyServerPatch(updated: OrderInfo) {
        // visible list
        val visible = _state.value.orders.toMutableList()
        val i = visible.indexOfFirst { it.id == updated.id }
        if (i != -1) {
            visible[i] = visible[i].copy(
                status = updated.status,
                details = updated.details ?: visible[i].details,
            )
            _state.update { it.copy(orders = visible) }
        }
        // backing list
        val j = allOrders.indexOfFirst { it.id == updated.id }
        if (j != -1) {
            allOrders[j] = allOrders[j].copy(
                status = updated.status,
                details = updated.details ?: allOrders[j].details,
            )
        }
    }
}

// -------- file-level helpers (not counted toward class function limit) --------

private fun currentFilteredFor(queryRaw: String, all: List<OrderInfo>): List<OrderInfo> {
    val q = queryRaw.trim()
    if (q.isBlank()) return all
    return all.filter { o ->
        o.orderNumber.contains(q, ignoreCase = true) ||
                o.name.contains(q, ignoreCase = true) ||
                (o.details?.contains(q, ignoreCase = true) == true)
    }
}

private fun MutableStateFlow<MyOrdersUiState>.publishFirstPageFrom(
    base: List<OrderInfo>,
    pageSize: Int,
    query: String
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
    pageSize: Int
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

private fun messageFor(e: Exception): String = when (e) {
    is HttpException -> "HTTP ${e.code()}"
    is UnknownHostException -> "No internet connection"
    is SocketTimeoutException -> "Request timed out"
    is IOException -> "Network error"
    else -> e.message ?: "Unknown error"
}

