package com.ntg.lmd.mainscreen.ui.screens

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.components.OrdersContentCallbacks
import com.ntg.lmd.mainscreen.ui.components.OrdersContentDeps
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
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel.OrderLogger
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModelFactory
import com.ntg.lmd.network.core.RetrofitProvider.userStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun myOrdersScreen(
    navController: NavController,
    onOpenOrderDetails: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as Application
    val ordersVm: MyOrdersViewModel = viewModel(factory = MyOrdersViewModelFactory(app))
    val updateVm: UpdateOrderStatusViewModel =
        viewModel(factory = UpdateOrderStatusViewModelFactory(app))
    val agentsVm: ActiveAgentsViewModel = viewModel(factory = ActiveAgentsViewModelFactory(app))
    val poolVm: MyPoolViewModel = viewModel(factory = MyPoolVMFactory())

    val snack = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val reassignOrderId = remember { mutableStateOf<String?>(null) }

    wireMyOrders(
        WireDeps(
            navController = navController,
            ordersVm = ordersVm,
            updateVm = updateVm,
            agentsVm = agentsVm,
            poolVm = poolVm,
            listState = listState,
            snack = snack,
            reassignOrderId = reassignOrderId,
        ),
    )

    ordersBody(
        OrdersBodyDeps(
            ordersVm = ordersVm,
            updateVm = updateVm,
            agentsVm = agentsVm,
            listState = listState,
            snack = snack,
            reassignOrderId = reassignOrderId,
            onOpenOrderDetails = onOpenOrderDetails,
        ),
    )
}

@Composable
private fun ordersBody(deps: OrdersBodyDeps) {
    val uiState by deps.ordersVm.uiState.collectAsState()
    val updatingIds by deps.updateVm.updatingIds.collectAsState()
    val agentsState by deps.agentsVm.state.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(deps.snack) },
        bottomBar = { bottomStickyButton(text = stringResource(R.string.order_pool)) {} },
    ) { innerPadding ->
        ordersContent(
            ordersVm = deps.ordersVm,
            deps = OrdersContentDeps(updateVm = deps.updateVm, listState = deps.listState, updatingIds = updatingIds),
            cbs =
                OrdersContentCallbacks(
                    onOpenOrderDetails = deps.onOpenOrderDetails,
                    onReassignRequested = { id ->
                        deps.reassignOrderId.value = id
                        deps.agentsVm.load()
                    },
                ),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }

    reassignBottomSheet(
        open = deps.reassignOrderId.value != null,
        state = agentsState,
        onDismiss = { deps.reassignOrderId.value = null },
        onRetry = { deps.agentsVm.load() },
        onSelect = { user ->
            val orderId = deps.reassignOrderId.value ?: return@reassignBottomSheet
            OrderLogger.uiTap(
                orderId,
                uiState.orders.firstOrNull { it.id == orderId }?.orderNumber,
                "Menu:Reassign→${user.name}",
            )
            deps.updateVm.update(orderId, OrderStatus.REASSIGNED, assignedAgentId = user.id)
            deps.reassignOrderId.value = null
        },
    )
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun wireMyOrders(deps: WireDeps) {
    val ctx = LocalContext.current
    val poolUi by deps.poolVm.ui.collectAsState()

    // Location + camera
    locationPermissionAndLastLocation(deps.poolVm)
    val mapStates = rememberMapStates()
    initialCameraPositionEffect(poolUi.orders, poolUi.selectedOrderNumber, mapStates)
    forwardMyPoolLocationToMyOrders(deps.poolVm, deps.ordersVm)

    // Current user
    val currentUserId: String? = remember { userStore.getUserId() }
    LaunchedEffect(currentUserId) { deps.ordersVm.listVM.setCurrentUserId(currentUserId) }

    // Effects + snackbar
    ordersEffects(
        vm = deps.ordersVm,
        listState = deps.listState,
        snackbarHostState = deps.snack,
        context = ctx,
    )

    val uiState by deps.ordersVm.uiState.collectAsState()
    LaunchedEffect(uiState.query) { deps.listState.scrollToItem(0) }

    LaunchedEffect(Unit) {
        deps.updateVm.success.collect { updated ->
            deps.ordersVm.statusVM.applyServerPatch(updated)
        }
    }
    LaunchedEffect(Unit) {
        deps.updateVm.error.collect { (msg, retry) ->
            val res =
                deps.snack.showSnackbar(
                    message = msg,
                    actionLabel = ctx.getString(R.string.retry),
                    withDismissAction = true,
                )
            if (res == SnackbarResult.ActionPerformed) retry()
        }
    }

    // Search observers
    observeOrdersSearch(deps.navController, deps.ordersVm)
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun forwardMyPoolLocationToMyOrders(
    poolVm: MyPoolViewModel,
    ordersVm: MyOrdersViewModel,
) {
    val lastLoc by poolVm.lastLocation.collectAsState(initial = null)
    LaunchedEffect(lastLoc) { ordersVm.listVM.updateDeviceLocation(lastLoc) }
}

// Handle search for orders
@Composable
private fun observeOrdersSearch(
    navController: NavController,
    vm: MyOrdersViewModel,
) {
    val back = navController.currentBackStackEntry

    // Launched Effects for Search
    LaunchedEffect(back) {
        val h = back?.savedStateHandle ?: return@LaunchedEffect
        combine(
            h.getStateFlow("searching", false),
            h.getStateFlow("search_text", ""),
        ) { enabled, text -> if (enabled) text else null }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { q ->
                vm.searchVM.applySearchQuery(q)
            }
    }

    LaunchedEffect(back) {
        val h = back?.savedStateHandle ?: return@LaunchedEffect
        h.getStateFlow("search_submit", "").collect { submitted ->
            if (submitted.isNotEmpty()) {
                vm.searchVM.applySearchQuery(submitted)
                h["search_submit"] = ""
            }
        }
    }
}

data class OrdersBodyDeps(
    val ordersVm: MyOrdersViewModel,
    val updateVm: UpdateOrderStatusViewModel,
    val agentsVm: ActiveAgentsViewModel,
    val listState: LazyListState,
    val snack: SnackbarHostState,
    val reassignOrderId: MutableState<String?>,
    val onOpenOrderDetails: (String) -> Unit,
)

data class WireDeps(
    val navController: NavController,
    val ordersVm: MyOrdersViewModel,
    val updateVm: UpdateOrderStatusViewModel,
    val agentsVm: ActiveAgentsViewModel,
    val poolVm: MyPoolViewModel,
    val listState: LazyListState,
    val snack: SnackbarHostState,
    val reassignOrderId: MutableState<String?>,
)
