package com.ntg.lmd.order.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.order.data.remote.repository.OrdersRepositoryImpl
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import com.ntg.lmd.order.domain.model.OrdersDialogsCallbacks
import com.ntg.lmd.order.domain.model.OrdersDialogsState
import com.ntg.lmd.order.domain.model.OrdersHistoryFilter
import com.ntg.lmd.order.domain.model.OrdersHistoryUiState
import com.ntg.lmd.order.domain.model.PagingState
import com.ntg.lmd.order.domain.model.defaultVerticalListConfig
import com.ntg.lmd.order.domain.model.usecase.GetOrdersUseCase
import com.ntg.lmd.order.ui.components.OrdersHistoryEffectsConfig
import com.ntg.lmd.order.ui.components.OrdersHistoryUiConfig
import com.ntg.lmd.order.ui.components.exportOrdersHistoryPdf
import com.ntg.lmd.order.ui.components.orderHistoryCard
import com.ntg.lmd.order.ui.components.ordersHistoryDialogs
import com.ntg.lmd.order.ui.components.ordersHistoryMenu
import com.ntg.lmd.order.ui.components.sharePdf
import com.ntg.lmd.order.ui.components.verticalListComponent
import com.ntg.lmd.order.ui.viewmodel.OrderHistoryViewModel
import com.ntg.lmd.order.ui.viewmodel.OrderHistoryViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ordersHistoryRoute(registerOpenMenu: ((() -> Unit) -> Unit)? = null) {
    val vm: OrderHistoryViewModel =
        viewModel(
            factory =
                OrderHistoryViewModelFactory(
                    GetOrdersUseCase(OrdersRepositoryImpl(RetrofitProvider.ordersHistoryApi)),
                ),
        )
    val token = RetrofitProvider.tokenStore.getAccessToken() ?: ""
    ordersHistoryStateHolders(vm, token, registerOpenMenu)
}

@Composable
private fun ordersHistoryStateHolders(
    vm: OrderHistoryViewModel,
    token: String,
    registerOpenMenu: ((() -> Unit) -> Unit)?,
) {
    val uiState = rememberOrdersUiState(vm)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    ordersHistoryEffects(
        OrdersHistoryEffectsConfig(
            vm,
            token,
            uiState.orders.size,
            uiState.listState,
            registerOpenMenu,
        ) {
            uiState.setMenuOpen(true)
        },
    )

    val config = rememberOrdersUiConfig(vm, token, uiState, ctx, scope)

    ordersHistoryUi(
        state = uiState.state,
        ctx = ctx,
        listState = uiState.listState,
        config = config,
        onRefresh = {
            // Close Filter Menu
            uiState.setShowFilterDialog(false)
            uiState.setShowSortDialog(false)
            uiState.setMenuOpen(false)

            // Reset then refresh
            vm.resetFilters()
            vm.refreshOrders(token)

            // Scroll to top
            scope.launch { uiState.listState.scrollToItem(0) }
        },
    )
}

@Composable
private fun rememberOrdersUiConfig(
    vm: OrderHistoryViewModel,
    token: String,
    uiState: OrdersUiStateHolder,
    ctx: Context,
    scope: CoroutineScope,
): OrdersHistoryUiConfig =
    OrdersHistoryUiConfig(
        showFilterDialog = uiState.showFilterDialog,
        showSortDialog = uiState.showSortDialog,
        menuOpen = uiState.menuOpen,
        filter = uiState.filter,
        onCloseMenu = { uiState.setMenuOpen(false) },
        onOpenFilter = { uiState.setShowFilterDialog(true) },
        onOpenSort = { uiState.setShowSortDialog(true) },
        onApplyFilter = { allowed ->
            scope.launch { uiState.listState.scrollToItem(0) }
            vm.setAllowedStatuses(allowed, token)
            uiState.setShowFilterDialog(false)
        },
        onApplySort = { asc ->
            scope.launch { uiState.listState.scrollToItem(0) }
            vm.setAgeAscending(asc, token)
            uiState.setShowSortDialog(false)
        },
        onExportPdf = {
            scope.launch(Dispatchers.IO) {
                val uri = exportOrdersHistoryPdf(ctx, uiState.orders)
                withContext(Dispatchers.Main) { uri?.let { sharePdf(ctx, it) } }
            }
        },
        onDismissFilter = { uiState.setShowFilterDialog(false) },
        onDismissSort = { uiState.setShowSortDialog(false) },
        onLoadMore = { vm.loadMoreOrders(token) },
    )

data class OrdersUiStateHolder(
    val state: OrdersHistoryUiState,
    val filter: OrdersHistoryFilter,
    val listState: LazyListState,
    val orders: List<OrderHistoryUi>,
    val showFilterDialog: Boolean,
    val setShowFilterDialog: (Boolean) -> Unit,
    val showSortDialog: Boolean,
    val setShowSortDialog: (Boolean) -> Unit,
    val menuOpen: Boolean,
    val setMenuOpen: (Boolean) -> Unit,
)

@Composable
private fun rememberOrdersUiState(vm: OrderHistoryViewModel): OrdersUiStateHolder {
    val orders by vm.orders.collectAsState(emptyList())
    val filter by vm.filter.collectAsState()
    val isLoadingMore by vm.isLoadingMore.collectAsState()
    val endReached by vm.endReached.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val listState = remember(filter.ageAscending, filter.allowed) { LazyListState(0, 0) }

    return OrdersUiStateHolder(
        state = OrdersHistoryUiState(orders, isLoadingMore, endReached, isRefreshing),
        filter = filter,
        listState = listState,
        orders = orders,
        showFilterDialog = showFilterDialog,
        setShowFilterDialog = { showFilterDialog = it },
        showSortDialog = showSortDialog,
        setShowSortDialog = { showSortDialog = it },
        menuOpen = menuOpen,
        setMenuOpen = { menuOpen = it },
    )
}

@Composable
private fun ordersHistoryEffects(config: OrdersHistoryEffectsConfig) {
    LaunchedEffect(config.token, config.ordersSize) {
        if (config.token.isNotBlank() && config.ordersSize == 0) config.vm.loadOrders(config.token)
    }
    LaunchedEffect(Unit) { config.registerOpenMenu?.invoke { config.openMenu() } }
}

@Composable
private fun ordersHistoryUi(
    state: OrdersHistoryUiState,
    ctx: Context,
    listState: LazyListState,
    config: OrdersHistoryUiConfig,
    onRefresh: () -> Unit,
) {
    val listConfig =
        defaultVerticalListConfig(
            listState = listState,
            paging = PagingState(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                isLoadingMore = state.isLoadingMore,
                endReached = state.endReached,
                onLoadMore = config.onLoadMore,
            ),
        ).copy(
            contentPadding =
                PaddingValues(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
        )

    verticalListComponent(
        items = state.orders,
        key = { order -> "${config.filter.ageAscending}:${config.filter.allowed}:${order.number}" },
        itemContent = { order -> orderHistoryCard(ctx, order) },
        config = listConfig,
    )

    ordersHistoryDialogs(
        state = OrdersDialogsState(config.showFilterDialog, config.showSortDialog, config.filter),
        callbacks =
            OrdersDialogsCallbacks(
                onFilterDismiss = config.onDismissFilter,
                onSortDismiss = config.onDismissSort,
                onApplyFilter = config.onApplyFilter,
                onApplySort = config.onApplySort,
            ),
    )

    ordersHistoryMenu(
        open = config.menuOpen,
        onClose = config.onCloseMenu,
        onFilterClick = config.onOpenFilter,
        onSortClick = config.onOpenSort,
        onExportPdfClick = config.onExportPdf,
    )
}

@Composable
fun statusBadge(
    text: String,
    color: Color,
) {
    Box(
        modifier = Modifier.padding(start = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
    }
}
