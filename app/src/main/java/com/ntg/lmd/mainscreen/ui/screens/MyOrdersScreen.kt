package com.ntg.lmd.mainscreen.ui.screens

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.navigation.NavController
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.data.repository.MyOrdersRepositoryImpl
import com.ntg.lmd.mainscreen.data.repository.UsersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.usecase.GetActiveUsersUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
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
    val updateVm: UpdateOrderStatusViewModel = viewModel(factory = UpdateOrderStatusViewModelFactory(app))
    val agentsVm: ActiveAgentsViewModel = viewModel(factory = ActiveAgentsViewModelFactory(app))
    val agentsState by agentsVm.state.collectAsState()
    var reassignOrderId by remember { mutableStateOf<String?>(null) }

    val poolVm: MyPoolViewModel = viewModel(factory = MyPoolVMFactory())
    val poolUi by poolVm.ui.collectAsState()

    locationPermissionAndLastLocation(poolVm)
    val mapStates = rememberMapStates()
    initialCameraPositionEffect(poolUi.orders, poolUi.selectedOrderNumber, mapStates)
    ForwardMyPoolLocationToMyOrders(poolVm = poolVm, ordersVm = ordersVm)

    val onReassignRequested: (String) -> Unit = { orderId ->
        reassignOrderId = orderId
        agentsVm.load()
    }

    val currentUserId: String? = remember {
        userStore.getUserId()
    }
    LaunchedEffect(currentUserId) {
        ordersVm.setCurrentUserId(currentUserId)
    }

    val state by ordersVm.state.collectAsState()
    val updatingIds by updateVm.updatingIds.collectAsState()
    val ctx = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    ordersEffects(ordersVm, state, listState, snack, ctx)

    // When the search query changes, jump back to the top of the list
    LaunchedEffect(state.query) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(Unit) {
        updateVm.success.collect { updated ->
            ordersVm.applyServerPatch(updated)
        }
    }
    LaunchedEffect(Unit) {
        updateVm.error.collect { (msg, retry) ->
            val res =
                snack.showSnackbar(
                    message = msg,
                    actionLabel = ctx.getString(R.string.retry),
                    withDismissAction = true,
                )
            if (res == SnackbarResult.ActionPerformed) retry()
        }
    }

    // Handle search for orders
    observeOrdersSearch(navController, ordersVm)

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = { bottomStickyButton(text = stringResource(R.string.order_pool)) {} },
    ) { innerPadding ->

        ordersContent(
            ordersVm = ordersVm,
            updateVm = updateVm,
            state = state,
            listState = listState,
            onOpenOrderDetails = onOpenOrderDetails,
            context = ctx,
            updatingIds = updatingIds,
            onReassignRequested = onReassignRequested,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        )
    }

    reassignBottomSheet(
        open = reassignOrderId != null,
        state = agentsState,
        onDismiss = { reassignOrderId = null },
        onRetry = { agentsVm.load() },
        onSelect = { user ->
            val orderId = reassignOrderId ?: return@reassignBottomSheet
            OrderLogger.uiTap(
                orderId,
                state.orders.firstOrNull { it.id == orderId }?.orderNumber,
                "Menu:Reassign→${user.name}"
            )
            updateVm.update(orderId, OrderStatus.REASSIGNED, assignedAgentId = user.id)
            reassignOrderId = null
        },
    )
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun ForwardMyPoolLocationToMyOrders(
    poolVm: MyPoolViewModel,
    ordersVm: MyOrdersViewModel,
) {
    val lastLoc by poolVm.lastLocation.collectAsState(initial = null)
    LaunchedEffect(lastLoc) {
        ordersVm.updateDeviceLocation(lastLoc)
    }
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
                vm.applySearchQuery(q)
            }
    }

    LaunchedEffect(back) {
        val h = back?.savedStateHandle ?: return@LaunchedEffect
        h.getStateFlow("search_submit", "").collect { submitted ->
            if (submitted.isNotEmpty()) {
                vm.applySearchQuery(submitted)
                h["search_submit"] = ""
            }
        }
    }
}

