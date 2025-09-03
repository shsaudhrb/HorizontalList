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
import kotlin.coroutines.cancellation.CancellationException

private const val PAGE_SIZE = 10

class MyOrdersViewModel(
    getMyOrders: GetMyOrdersUseCase,
    private val computeDistancesUseCase: ComputeDistancesUseCase,
    initialUserId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(MyOrdersUiState(isLoading = false))
    val state: StateFlow<MyOrdersUiState> = _state.asStateFlow()

    private val currentUserId = MutableStateFlow<String?>(initialUserId)
    private val deviceLocation = MutableStateFlow<Location?>(null)
    private val paginator = OrdersPaginator(getMyOrders, PAGE_SIZE)

    init { refreshOrders() }

    fun updateDeviceLocation(location: Location?) {
        deviceLocation.value = location
        if (location != null && _state.value.orders.isNotEmpty()) {
            val computed = withDistances(_state.value.orders, location, computeDistancesUseCase)
            _state.update { it.copy(orders = computed) }
        }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val bundle = paginator.loadFirst(currentUserId.value)
                val filtered = applyDisplayFilter(bundle.combined, state.value.query, currentUserId.value)
                val withDist = withDistances(filtered, deviceLocation.value, computeDistancesUseCase)
                _state.publishFirstPageFrom(withDist, PAGE_SIZE, state.value.query)
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

    fun loadOrders(context: Context) {
        val alreadyHasData = _state.value.orders.isNotEmpty()
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = !alreadyHasData, errorMessage = null, emptyMessage = null) }

        viewModelScope.launch {
            try {
                val bundle = paginator.loadFirst(currentUserId.value)
                val filtered = applyDisplayFilter(bundle.combined, state.value.query, currentUserId.value)
                val withDist = withDistances(filtered, deviceLocation.value, computeDistancesUseCase)
                _state.publishFirstPageFrom(withDist, PAGE_SIZE, state.value.query)
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
    @Suppress("ReturnCount")
    fun loadNextPage(context: Context) {
        val s = state.value
        if (s.isLoading || s.isLoadingMore) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                val bundle = paginator.loadNext(currentUserId.value)
                if (bundle == null) {
                    _state.update { it.copy(isLoadingMore = false) }
                    return@launch
                }
                val filtered = applyDisplayFilter(bundle.combined, state.value.query, currentUserId.value)
                val withDist = withDistances(filtered, deviceLocation.value, computeDistancesUseCase)
                _state.publishAppendFrom(withDist, bundle.page, PAGE_SIZE)
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

    fun refresh(context: Context) {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val bundle = paginator.loadFirst(currentUserId.value)
                val filtered = applyDisplayFilter(bundle.combined, state.value.query, currentUserId.value)
                val withDist = withDistances(filtered, deviceLocation.value, computeDistancesUseCase)
                _state.publishFirstPageFrom(withDist, PAGE_SIZE, state.value.query)
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

    fun setCurrentUserId(id: String?) {
        currentUserId.value = id
        val filtered = applyDisplayFilter(paginator.snapshot(), state.value.query, id)
        val withDist = withDistances(filtered, deviceLocation.value, computeDistancesUseCase)
        _state.update { it.copy(orders = withDist) }
    }

    fun updateStatusLocally(id: String, newStatus: OrderStatus) {
        val updated = state.value.orders.map { o -> if (o.id == id) o.copy(status = newStatus) else o }
        _state.update { it.copy(orders = updated) }
    }

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
        // paginator snapshot
        paginator.applyServerPatch(updated)
    }
}
