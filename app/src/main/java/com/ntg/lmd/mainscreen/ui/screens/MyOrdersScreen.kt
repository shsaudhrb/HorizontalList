package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.components.bottomStickyButton
import com.ntg.lmd.mainscreen.ui.components.callButton
import com.ntg.lmd.mainscreen.ui.components.deliverDialog
import com.ntg.lmd.mainscreen.ui.components.emptyView
import com.ntg.lmd.mainscreen.ui.components.errorView
import com.ntg.lmd.mainscreen.ui.components.infoBanner
import com.ntg.lmd.mainscreen.ui.components.loadingView
import com.ntg.lmd.mainscreen.ui.components.orderHeaderWithMenu
import com.ntg.lmd.mainscreen.ui.components.primaryActionButton
import com.ntg.lmd.mainscreen.ui.components.reasonDialog
import com.ntg.lmd.mainscreen.ui.components.reassignDialog
import com.ntg.lmd.mainscreen.ui.components.simpleConfirmDialog
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.model.MyOrdersUiState
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import com.ntg.lmd.ui.theme.SuccessGreen
import kotlinx.coroutines.flow.collectLatest

private const val VISIBLE_THRESHOLD = 3
private val CARD_ELEVATION = 3.dp

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UnusedParameter")
@Composable
fun myOrdersScreen(
    // Root screen: wires VM, effects, Scaffold, and pull-to-refresh around the list
    navController: NavController,
    externalQuery: String,
    onOpenOrderDetails: (String) -> Unit,
) {
    val vm: MyOrdersViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // external query effect kept here (so we don’t add a param to the effects helper)
    LaunchedEffect(externalQuery) { vm.onQueryChange(externalQuery) }

    ordersEffects(
        vm = vm,
        state = state,
        listState = listState,
        snackbarHostState = snackbarHostState,
        context = context,
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { bottomStickyButton(text = stringResource(R.string.order_pool)) { } },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { vm.refresh() },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            ordersContent(
                state = state,
                listState = listState,
                onOpenOrderDetails = onOpenOrderDetails,
                context = context,
            )
        }
    }
}

@Composable
private fun ordersEffects(
    // Side effects: initial load, listen to status/error events, and trigger infinite scroll
    vm: MyOrdersViewModel,
    state: MyOrdersUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context,
) {
    LaunchedEffect(Unit) { vm.loadOrders() }

    LaunchedEffect(Unit) {
        LocalUiOnlyStatusBus.statusEvents.collectLatest { (id, newStatus) ->
            vm.updateStatusLocally(id, newStatus)
        }
    }
    LaunchedEffect(Unit) {
        LocalUiOnlyStatusBus.errorEvents.collectLatest { (msg, retry) ->
            val result =
                snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = retry?.let { context.getString(R.string.retry) },
                    withDismissAction = true,
                )
            if (result == SnackbarResult.ActionPerformed) retry?.invoke()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - VISIBLE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore, state.isRefreshing, state.isLoading) {
        if (shouldLoadMore && !state.isRefreshing && !state.isLoading) vm.loadNextPage()
    }
}

@Composable
private fun ordersContent(
    // Chooses what to render (banner/loading/error/empty/list) and hooks up item actions
    state: MyOrdersUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpenOrderDetails: (String) -> Unit,
    context: android.content.Context,
) {
    val vm: MyOrdersViewModel = viewModel()
    Column(Modifier.fillMaxSize()) {
        if (!state.isGpsAvailable) {
            infoBanner(
                text = stringResource(R.string.distance_unavailable_gps),
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.mediumSpace)),
            )
        }
        when {
            state.isLoading && state.orders.isEmpty() -> loadingView()
            state.errorMessage != null ->
                errorView(state.errorMessage!!) {
                    vm.retry()
                }

            state.emptyMessage != null -> emptyView(state.emptyMessage!!)
            else ->
                orderList(
                    orders = state.orders,
                    listState = listState,
                    actions =
                        OrderActions(
                            onDetails = { orderId -> onOpenOrderDetails(orderId) },
                            onConfirmOrPick = { _ -> },
                            onCall = { id ->
                                val order = state.orders.firstOrNull { it.id == id }
                                val phone = order?.customerPhone
                                if (!phone.isNullOrBlank()) {
                                    val intent =
                                        android.content.Intent(
                                            android.content.Intent.ACTION_DIAL,
                                            android.net.Uri.parse("tel:$phone"),
                                        )
                                    context.startActivity(intent)
                                } else {
                                    LocalUiOnlyStatusBus.errorEvents.tryEmit(
                                        context.getString(R.string.phone_missing) to null,
                                    )
                                }
                            },
                        ),
                    isLoadingMore = state.isLoadingMore,
                )
        }
    }
}

@Composable
fun orderList(
    // Lazy list of orders with a paging spinner item when loading more
    orders: List<OrderInfo>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    actions: OrderActions,
    isLoadingMore: Boolean,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(dimensionResource(R.dimen.mediumSpace)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.mediumSpace)),
    ) {
        items(items = orders, key = { it.id }) { order ->
            myOrderCard(
                order = order,
                onDetails = { actions.onDetails(order.id) },
                onConfirmOrPick = { actions.onConfirmOrPick(order.id) },
                onCall = { actions.onCall(order.id) },
            )
        }
        if (isLoadingMore) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.mediumSpace)),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
        item { Spacer(modifier = Modifier.height(dimensionResource(R.dimen.extraSmallSpace))) }
    }
}

@Suppress("UnusedParameter")
@Composable
fun myOrderCard(
    // Single order card
    modifier: Modifier = Modifier,
    order: OrderInfo,
    onDetails: () -> Unit,
    onConfirmOrPick: () -> Unit,
    onCall: () -> Unit,
) {
    val context = LocalContext.current
    var showReassign by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius)),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.largeSpace))) {
            // Header with 3-dots menu
            orderHeaderWithMenu(
                order = order,
                onPickUp = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(
                        order.id to OrderStatus.PICKUP,
                    )
                },
                onCancel = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(order.id to OrderStatus.CANCELED)
                },
                onReassign = {
                    showReassign = true
                },
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mediumSpace)))

            // Main action row
            orderActionsRow(
                order = order,
                onDetails = onDetails,
            )

            callButton(onCall)
        }
    }

    if (showReassign) {
        reassignDialog(
            onDismiss = { showReassign = false },
            onConfirm = { assignee ->
                LocalUiOnlyStatusBus.errorEvents.tryEmit(
                    context.getString(R.string.reassign_order) + " → " + assignee to null,
                )
                showReassign = false
            },
        )
    }
}

sealed class ActionDialog {
    data object Confirm : ActionDialog()

    data object PickUp : ActionDialog()

    data object Start : ActionDialog()

    data object Deliver : ActionDialog()

    data object Fail : ActionDialog()
}

@Composable
fun orderActionsRow(
    // keeps which dialog is open in local state
    order: OrderInfo,
    onDetails: () -> Unit,
) {
    var dialog by remember { mutableStateOf<ActionDialog?>(null) }

    actionPrimaryRow(
        status = order.status,
        onDetails = onDetails,
        onAction = { dialog = it },
    )

    secondaryFailRow(
        status = order.status,
        onFailClick = { dialog = ActionDialog.Fail },
    )

    actionDialogs(
        dialog = dialog,
        orderId = order.id,
        onDismiss = { dialog = null },
    )
}

@Composable
fun actionPrimaryRow(
    // First action row: details button + primary action based on current order status
    status: OrderStatus,
    onDetails: () -> Unit,
    onAction: (ActionDialog) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace))) {
        OutlinedButton(
            onClick = onDetails,
            modifier = Modifier.weight(0.8f),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
        ) {
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.smallerSpace)))
            Text(
                text = stringResource(id = R.string.order_details),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
        }

        when (status) {
            OrderStatus.ADDED ->
                primaryActionButton(
                    text = stringResource(R.string.confirm_order),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(ActionDialog.Confirm) },
                )

            OrderStatus.CONFIRMED ->
                primaryActionButton(
                    text = stringResource(R.string.pick_order),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(ActionDialog.PickUp) },
                )

            OrderStatus.PICKUP ->
                primaryActionButton(
                    text = stringResource(R.string.start_delivery),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(ActionDialog.Start) },
                )

            OrderStatus.START_DELIVERY ->
                primaryActionButton(
                    text = stringResource(R.string.deliver_order),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(ActionDialog.Deliver) },
                )

            else -> {}
        }
    }
}

@Composable
fun secondaryFailRow(
    // Optional second row: shows “Delivery failed” button for dispatched/delivering states.
    status: OrderStatus,
    onFailClick: () -> Unit,
) {
    Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))
    if (status == OrderStatus.PICKUP || status == OrderStatus.START_DELIVERY) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace))) {
            OutlinedButton(
                onClick = onFailClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
            ) { Text(stringResource(R.string.delivery_failed)) }
        }
    }
}

@Composable
fun actionDialogs(
    // Shows the appropriate confirm/reason dialogs
    dialog: ActionDialog?,
    orderId: String,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        ActionDialog.Confirm ->
            simpleConfirmDialog(
                title = stringResource(R.string.confirm_order),
                onDismiss = onDismiss,
                onConfirm = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(orderId to OrderStatus.CONFIRMED)
                    onDismiss()
                },
            )

        ActionDialog.PickUp ->
            simpleConfirmDialog(
                title = stringResource(R.string.pick_order),
                onDismiss = onDismiss,
                onConfirm = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(orderId to OrderStatus.PICKUP)
                    onDismiss()
                },
            )

        ActionDialog.Start ->
            simpleConfirmDialog(
                title = stringResource(R.string.start_delivery),
                onDismiss = onDismiss,
                onConfirm = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(orderId to OrderStatus.START_DELIVERY)
                    onDismiss()
                },
            )

        ActionDialog.Deliver ->
            deliverDialog(
                onDismiss = onDismiss,
                onConfirm = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(orderId to OrderStatus.DELIVERY_DONE)
                    onDismiss()
                },
            )

        ActionDialog.Fail ->
            reasonDialog(
                title = stringResource(R.string.delivery_failed),
                onDismiss = onDismiss,
                onConfirm = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(orderId to OrderStatus.DELIVERY_FAILED)
                    onDismiss()
                },
            )

        null -> {}
    }
}

data class OrderActions(
    val onDetails: (String) -> Unit,
    val onConfirmOrPick: (String) -> Unit,
    val onCall: (String) -> Unit,
)

@Composable
fun statusTint(status: String) =
    // Returns the tint color for the status label (green for added/confirmed,
    if (status.equals("confirmed", ignoreCase = true) ||
        status.equals("added", ignoreCase = true)
    ) {
        SuccessGreen
    } else {
        MaterialTheme.colorScheme.onSurface
    }
