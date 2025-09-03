package com.ntg.lmd.mainscreen.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.components.OrdersContentParams
import com.ntg.lmd.mainscreen.ui.components.bottomStickyButton
import com.ntg.lmd.mainscreen.ui.components.initialCameraPositionEffect
import com.ntg.lmd.mainscreen.ui.components.locationPermissionAndLastLocation
import com.ntg.lmd.mainscreen.ui.components.ordersContent
import com.ntg.lmd.mainscreen.ui.components.ordersEffects
import com.ntg.lmd.mainscreen.ui.components.reassignBottomSheet
import com.ntg.lmd.mainscreen.ui.viewmodel.ActiveAgentsViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.ActiveAgentsViewModelFactory
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModelFactory
import com.ntg.lmd.mainscreen.ui.viewmodel.MyPoolVMFactory
import com.ntg.lmd.mainscreen.ui.viewmodel.MyPoolViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModelFactory
import com.ntg.lmd.network.core.RetrofitProvider.userStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun myOrdersScreen(onOpenOrderDetails: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as Application

    val ordersVm: MyOrdersViewModel = viewModel(factory = MyOrdersViewModelFactory(app))
    val updateVm: UpdateOrderStatusViewModel = viewModel(factory = UpdateOrderStatusViewModelFactory(app))
    val agentsVm: ActiveAgentsViewModel = viewModel(factory = ActiveAgentsViewModelFactory(app))
    val poolVm: MyPoolViewModel = viewModel(factory = MyPoolVMFactory())

    myOrdersScreenInner(
        ordersVm = ordersVm,
        updateVm = updateVm,
        agentsVm = agentsVm,
        poolVm = poolVm,
        onOpenOrderDetails = onOpenOrderDetails,
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun myOrdersScreenInner(
    ordersVm: MyOrdersViewModel,
    updateVm: UpdateOrderStatusViewModel,
    agentsVm: ActiveAgentsViewModel,
    poolVm: MyPoolViewModel,
    onOpenOrderDetails: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()

    val ui by ordersVm.state.collectAsState()
    val updatingIds by updateVm.updatingIds.collectAsState()

    setupMapAndLocation(poolVm, ordersVm)
    connectCurrentUserToOrders(ordersVm)
    ordersEffectsHost(ordersVm, ui, listState, snack, ctx)
    updateVmCollectors(updateVm, ordersVm, snack, ctx)

    reassignHost(ui = ui, agentsVm = agentsVm, updateVm = updateVm) { onReassignRequested ->
        ordersScaffold(
            isRefreshing = ui.isRefreshing,
            onRefresh = { ordersVm.refresh(ctx) },
            pullState = pullState,
            snack = snack,
        ) {
            ordersContent(
                OrdersContentParams(
                    ordersVm = ordersVm,
                    updateVm = updateVm,
                    listState = listState,
                    onOpenOrderDetails = onOpenOrderDetails,
                    updatingIds = updatingIds,
                    onReassignRequested = onReassignRequested,
                )
            )
        }
    }
}
@Composable
private fun reassignHost(
    ui: com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState,
    agentsVm: ActiveAgentsViewModel,
    updateVm: UpdateOrderStatusViewModel,
    content: @Composable (onReassignRequested: (String) -> Unit) -> Unit,
) {
    val agentsState by agentsVm.state.collectAsState()
    var reassignOrderId by remember { mutableStateOf<String?>(null) }

    val onReassignRequested: (String) -> Unit = {
        reassignOrderId = it
        agentsVm.load()
    }

    content(onReassignRequested)

    reassignBottomSheetHost(
        open = reassignOrderId != null,
        agentsState = agentsState,
        onDismiss = { reassignOrderId = null },
        onRetry = { agentsVm.load() },
    ) { user ->
        val id = reassignOrderId ?: return@reassignBottomSheetHost
        UpdateOrderStatusViewModel.OrderLogger.uiTap(
            id,
            ui.orders.firstOrNull { it.id == id }?.orderNumber,
            "Menu:Reassign→${user.name}",
        )
        updateVm.update(id, OrderStatus.REASSIGNED, assignedAgentId = user.id)
        reassignOrderId = null
    }
}

@Composable
private fun setupMapAndLocation(poolVm: MyPoolViewModel, ordersVm: MyOrdersViewModel) {
    locationPermissionAndLastLocation(poolVm)
    val mapStates = rememberMapStates()
    val poolUi by poolVm.ui.collectAsState()
    initialCameraPositionEffect(poolUi.orders, poolUi.selectedOrderNumber, mapStates)
    forwardMyPoolLocationToMyOrders(poolVm, ordersVm)
}

@Composable
private fun connectCurrentUserToOrders(ordersVm: MyOrdersViewModel) {
    val currentUserId = remember { userStore.getUserId() }
    LaunchedEffect(currentUserId) { ordersVm.setCurrentUserId(currentUserId) }
}

@Composable
private fun ordersEffectsHost(
    ordersVm: MyOrdersViewModel,
    state: com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    snack: SnackbarHostState,
    ctx: android.content.Context,
) {
    ordersEffects(ordersVm, state, listState, snack, ctx)
}

@Composable
private fun updateVmCollectors(
    updateVm: UpdateOrderStatusViewModel,
    ordersVm: MyOrdersViewModel,
    snack: SnackbarHostState,
    ctx: android.content.Context,
) {
    LaunchedEffect(Unit) { updateVm.success.collect { ordersVm.applyServerPatch(it) } }
    LaunchedEffect(Unit) {
        updateVm.error.collect { (msg, retry) ->
            val res = snack.showSnackbar(
                message = msg,
                actionLabel = ctx.getString(R.string.retry),
                withDismissAction = true,
            )
            if (res == SnackbarResult.ActionPerformed) retry()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ordersScaffold(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: androidx.compose.material3.pulltorefresh.PullToRefreshState,
    snack: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = { bottomStickyButton(text = stringResource(R.string.order_pool)) {} },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) { content() }
    }
}

@Composable
private fun reassignBottomSheetHost(
    open: Boolean,
    agentsState: com.ntg.lmd.mainscreen.ui.viewmodel.AgentsState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (com.ntg.lmd.mainscreen.domain.model.ActiveUser) -> Unit,
) {
    reassignBottomSheet(
        open = open,
        state = agentsState,
        onDismiss = onDismiss,
        onRetry = onRetry,
        onSelect = onSelect,
    )
}

@Composable
private fun forwardMyPoolLocationToMyOrders(
    poolVm: MyPoolViewModel,
    ordersVm: MyOrdersViewModel,
) {
    val lastLoc by poolVm.lastLocation.collectAsState(initial = null)
    LaunchedEffect(lastLoc) { ordersVm.updateDeviceLocation(lastLoc) }
}
