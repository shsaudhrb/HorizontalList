package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.cancellation.CancellationException

class OrdersListController(
    private val store: OrdersStore,
    private val helpers: OrdersListHelpers,
    private val getMyOrders: GetMyOrdersUseCase,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val NEXT_PAGE_ERROR_COOLDOWN_MS = 5_000L
    }

    private val state get() = store.state
    private val currentUserId get() = store.currentUserId
    private val deviceLocation get() = store.deviceLocation
    private val allOrders get() = store.allOrders

    fun setCurrentUserId(id: String?) {
        currentUserId.value = id
        val display = helpers.computeDisplay(deviceLocation.value, allOrders, state.value.query, id)
        state.update { it.copy(orders = display) }
    }

    fun loadInitial(context: Context) {
        val alreadyHasData = state.value.orders.isNotEmpty()
        if (state.value.isLoading) return

        state.update {
            it.copy(
                isLoading = !alreadyHasData,
                errorMessage = null,
                emptyMessage = null,
            )
        }

        scope.launch {
            try {
                val (items, endReached) = fetchFirstPage(bypassCache = true)
                applyAndPublishFirstPage(items, endReached)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: SocketTimeoutException) {
                handleInitialError(e, alreadyHasData, context) { loadInitial(context) }
            } catch (e: IOException) {
                handleInitialError(e, alreadyHasData, context) { loadInitial(context) }
            } catch (e: HttpException) {
                postError(e.toUserMessage()) { loadInitial(context) }
            } catch (e: SSLHandshakeException) {
                postError(e.toUserMessage()) { loadInitial(context) }
            } catch (e: UnknownHostException) {
                postError(e.toUserMessage()) { loadInitial(context) }
            } finally {
                state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh(context: Context) {
        val s = state.value
        if (s.isRefreshing) return
        state.update { it.copy(isRefreshing = true, errorMessage = null) }

        scope.launch {
            try {
                val (items, endReached) = fetchFirstPage(bypassCache = true)
                applyAndPublishFirstPage(items, endReached)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: SocketTimeoutException) {
                postError(e.toUserMessage()) { refresh(context) }
            } catch (e: IOException) {
                postError(e.toUserMessage()) { refresh(context) }
            } catch (e: HttpException) {
                postError(e.toUserMessage()) { refresh(context) }
            } catch (e: SSLHandshakeException) {
                postError(e.toUserMessage()) { refresh(context) }
            } catch (e: UnknownHostException) {
                postError(e.toUserMessage()) { refresh(context) }
            } finally {
                state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshStrict() {
        if (state.value.isLoading) return
        scope.launch {
            state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { fetchFirstPage(bypassCache = true) }
                .onSuccess { (items, endReached) -> applyAndPublishFirstPage(items, endReached) }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = e.toUserMessage(),
                        )
                    }
                }
        }
    }

    private var lastNextPageErrorAtMs: Long = 0L
    private var lastFailedPage: Int? = null

    fun loadNextPage(context: Context) {
        val s = state.value
        val nextPageNum = store.page + 1

        if (lastFailedPage == nextPageNum &&
            (System.currentTimeMillis() - lastNextPageErrorAtMs) < NEXT_PAGE_ERROR_COOLDOWN_MS
        ) {
            return
        }
        if (s.isLoading || s.isLoadingMore || store.endReached) return

        scope.launch {
            state.update { it.copy(isLoadingMore = true) }
            try {
                val nextItems = getPage(page = nextPageNum, bypassCache = true)
                store.endReached = nextItems.isEmpty() || nextItems.size < OrdersPaging.PAGE_SIZE
                store.page = nextPageNum

                allOrders.addAll(nextItems)
                applyAndPublishAppend()
                clearNextPageErrorMark()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: SocketTimeoutException) {
                markNextPageError(nextPageNum)
                postError(e.toUserMessage()) { loadNextPage(context) }
            } catch (e: IOException) {
                markNextPageError(nextPageNum)
                postError(e.toUserMessage()) { loadNextPage(context) }
            } catch (e: HttpException) {
                markNextPageError(nextPageNum)
                postError(e.toUserMessage()) { loadNextPage(context) }
            } catch (e: UnknownHostException) {
                markNextPageError(nextPageNum)
                postError(e.toUserMessage()) { loadNextPage(context) }
            } finally {
                state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private suspend fun fetchFirstPage(bypassCache: Boolean): Pair<List<OrderInfo>, Boolean> {
        store.page = 1
        store.endReached = false
        allOrders.clear()

        val first = getPage(page = 1, bypassCache = bypassCache)
        allOrders.addAll(first)
        store.endReached = first.size < OrdersPaging.PAGE_SIZE

        return allOrders.toList() to store.endReached
    }

    private suspend fun getPage(
        page: Int,
        bypassCache: Boolean,
    ): List<OrderInfo> {
        val uid = currentUserId.value
        val res =
            getMyOrders(
                page = page,
                limit = OrdersPaging.PAGE_SIZE,
                bypassCache = bypassCache,
                assignedAgentId = uid,
                userOrdersOnly = true,
            )
        return res.items
    }

    private fun applyAndPublishFirstPage(
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

    private fun applyAndPublishAppend() {
        val uid = currentUserId.value
        val display =
            helpers.computeDisplay(deviceLocation.value, allOrders, state.value.query, uid)
        helpers.publishAppendFrom(
            state = state,
            base = display,
            page = store.page,
            pageSize = OrdersPaging.PAGE_SIZE,
            endReached = store.endReached,
        )
    }

    private fun handleInitialError(
        e: IOException,
        alreadyHasData: Boolean,
        context: Context,
        retry: () -> Unit,
    ) {
        val msg = e.toUserMessage()
        if (alreadyHasData) {
            state.update { it.copy(isLoading = false, errorMessage = msg) }
            LocalUiOnlyStatusBus.errorEvents.tryEmit(msg to retry)
        } else {
            helpers.handleInitialLoadError(
                e = e,
                alreadyHasData = alreadyHasData,
                context = context,
                state = state,
                retry = { ctx -> loadInitial(ctx) },
            )
        }
    }

    private fun postError(
        message: String,
        retry: () -> Unit,
    ) {
        state.update { it.copy(errorMessage = message) }
        LocalUiOnlyStatusBus.errorEvents.tryEmit(message to retry)
    }

    private fun markNextPageError(page: Int) {
        lastFailedPage = page
        lastNextPageErrorAtMs = System.currentTimeMillis()
    }

    private fun clearNextPageErrorMark() {
        lastFailedPage = null
        lastNextPageErrorAtMs = 0L
    }
}
