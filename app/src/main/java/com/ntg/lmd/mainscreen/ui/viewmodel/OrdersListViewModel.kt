package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

class OrdersListViewModel(
    private val store: OrdersStore,
    private val getMyOrders: GetMyOrdersUseCase,
    computeDistancesUseCase: ComputeDistancesUseCase,
) : ViewModel() {
    private val state get() = store.state
    private val currentUserId get() = store.currentUserId
    private val deviceLocation get() = store.deviceLocation
    private val allOrders get() = store.allOrders

    // NEW: helper instance
    private val helpers = OrdersListHelpers(store, computeDistancesUseCase)

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun updateDeviceLocation(location: Location?) {
        deviceLocation.value = location
        if (location != null && state.value.orders.isNotEmpty()) {
            val computed = helpers.withDistances(location, state.value.orders)
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
                        limit = OrdersPaging.PAGE_SIZE, bypassCache = true,
                        assignedAgentId = uid,
                        userOrdersOnly = true,
                    )

                allOrders.clear()
                allOrders.addAll(page1.items)
                store.endReached = page1.rawCount < OrdersPaging.PAGE_SIZE

                val display = helpers.applyDisplayFilter(allOrders, state.value.query, uid)
                val withDist = helpers.withDistances(deviceLocation.value, display)
                helpers.publishFirstPageFrom(
                    state,
                    withDist,
                    OrdersPaging.PAGE_SIZE,
                    state.value.query,
                    store.endReached,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: IOException) {
                state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = helpers.messageFor(e),
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

                val display = helpers.applyDisplayFilter(allOrders, state.value.query, uid)
                val withDist = helpers.withDistances(deviceLocation.value, display)
                helpers.publishFirstPageFrom(
                    state,
                    withDist,
                    OrdersPaging.PAGE_SIZE,
                    state.value.query,
                    store.endReached,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: IOException) {
                helpers.handleInitialLoadError(e, alreadyHasData, context, state, ::loadOrders)
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

                val display = helpers.applyDisplayFilter(allOrders, state.value.query, uid)
                val withDist = helpers.withDistances(deviceLocation.value, display)
                helpers.publishFirstPageFrom(
                    state,
                    withDist,
                    OrdersPaging.PAGE_SIZE,
                    state.value.query,
                    store.endReached,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: SocketTimeoutException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(helpers.messageFor(e) to { refresh(context) })
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
                val display = helpers.applyDisplayFilter(allOrders, state.value.query, uid)
                val withDist = helpers.withDistances(deviceLocation.value, display)
                helpers.publishAppendFrom(
                    state,
                    withDist,
                    store.page,
                    OrdersPaging.PAGE_SIZE,
                    store.endReached,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: IOException) {
                LocalUiOnlyStatusBus.errorEvents.tryEmit(helpers.messageFor(e) to { refresh(context) })
            } finally {
                state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun setCurrentUserId(id: String?) {
        store.currentUserId.value = id
        val display = helpers.applyDisplayFilter(allOrders, state.value.query, id)
        val withDist = helpers.withDistances(deviceLocation.value, display)
        state.update { it.copy(orders = withDist) }
    }
}
