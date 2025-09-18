package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import kotlinx.coroutines.flow.update
import java.io.IOException

class OrdersListErrorHandler(
    private val store: OrdersStore,
    private val helpers: OrdersListHelpers,
) {
    private val state get() = store.state

    fun handleInitialError(
        e: IOException,
        alreadyHasData: Boolean,
        context: Context,
        retryInitial: (Context) -> Unit,
    ) {
        val msg = e.toUserMessage()
        if (alreadyHasData) {
            state.update { it.copy(isLoading = false, errorMessage = msg) }
            LocalUiOnlyStatusBus.errorEvents.tryEmit(msg to { retryInitial(context) })
        } else {
            helpers.handleInitialLoadError(
                e = e,
                alreadyHasData = alreadyHasData,
                context = context,
                state = state,
                retry = retryInitial,
            )
        }
    }

    fun postError(
        message: String,
        retry: () -> Unit,
    ) {
        state.update { it.copy(errorMessage = message) }
        LocalUiOnlyStatusBus.errorEvents.tryEmit(message to retry)
    }
}

class OrdersPager(
    private val getMyOrders: GetMyOrdersUseCase,
) {
    suspend fun getPage(
        page: Int,
        bypassCache: Boolean,
        assignedAgentId: String?,
        userOrdersOnly: Boolean = true,
        limit: Int = OrdersPaging.PAGE_SIZE,
    ): List<OrderInfo> {
        val res =
            getMyOrders(
                page = page,
                limit = limit,
                bypassCache = bypassCache,
                assignedAgentId = assignedAgentId,
                userOrdersOnly = userOrdersOnly,
            )
        return res.items
    }
}

class OrdersThrottle(
    private val cooldownMs: Long = 5_000L,
) {
    private var lastNextPageErrorAtMs: Long = 0L
    private var lastFailedPage: Int? = null

    fun canRequest(page: Int): Boolean {
        val withinCooldown =
            lastFailedPage == page &&
                (System.currentTimeMillis() - lastNextPageErrorAtMs) < cooldownMs
        return !withinCooldown
    }

    fun markError(page: Int) {
        lastFailedPage = page
        lastNextPageErrorAtMs = System.currentTimeMillis()
    }

    fun clear() {
        lastFailedPage = null
        lastNextPageErrorAtMs = 0L
    }
}

class OrdersListPublisher(
    private val store: OrdersStore,
    private val helpers: OrdersListHelpers,
) {
    private val state get() = store.state
    private val deviceLocation get() = store.deviceLocation
    private val allOrders get() = store.allOrders
    private val currentUserId get() = store.currentUserId

    fun setCurrentUserIdAndRecompute(id: String?) {
        currentUserId.value = id
        val display = helpers.computeDisplay(deviceLocation.value, allOrders, state.value.query, id)
        state.update { it.copy(orders = display) }
    }

    fun publishFirstPage(
        items: List<OrderInfo>,
        endReached: Boolean,
    ) {
        val uid = currentUserId.value
        val display = helpers.computeDisplay(deviceLocation.value, items, state.value.query, uid)
        helpers.publishFirstPageFrom(
            state = state,
            base = display,
            pageSize = OrdersPaging.PAGE_SIZE,
            query = state.value.query,
            endReached = endReached,
        )
    }

    fun publishAppend() {
        val uid = currentUserId.value
        val display = helpers.computeDisplay(deviceLocation.value, store.allOrders, state.value.query, uid)
        helpers.publishAppendFrom(
            state = state,
            base = display,
            page = store.page,
            pageSize = OrdersPaging.PAGE_SIZE,
            endReached = store.endReached,
        )
    }
}
