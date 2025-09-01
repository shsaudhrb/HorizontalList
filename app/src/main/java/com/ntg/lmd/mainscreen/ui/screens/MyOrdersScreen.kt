package com.ntg.lmd.mainscreen.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.ntg.lmd.mainscreen.data.repository.MyOrdersRepositoryImpl
import com.ntg.lmd.mainscreen.data.repository.UpdateOrdersStatusRepository
import com.ntg.lmd.mainscreen.data.repository.UsersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.usecase.GetActiveUsersUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.domain.usecase.UpdateOrderStatusUseCase
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
import com.ntg.lmd.mainscreen.ui.components.reassignBottomSheet
import com.ntg.lmd.mainscreen.ui.components.reassignDialog
import com.ntg.lmd.mainscreen.ui.components.simpleConfirmDialog
import com.ntg.lmd.mainscreen.ui.screens.orders.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
import com.ntg.lmd.mainscreen.ui.viewmodel.ActiveAgentsViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.ActiveAgentsViewModelFactory
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModelFactory
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel.OrderLogger
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModelFactory
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.ui.theme.SuccessGreen
import com.ntg.lmd.utils.SecureUserStore
import kotlinx.coroutines.flow.collectLatest

private const val VISIBLE_THRESHOLD = 3
private val CARD_ELEVATION = 3.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun myOrdersScreen(
    onOpenOrderDetails: (String) -> Unit,
) {
    val repo = remember { MyOrdersRepositoryImpl(RetrofitProvider.ordersApi) }
    val updaterepo = remember { UpdateOrdersStatusRepository(RetrofitProvider.updateStatusApi) }

    val getUsecase = remember { GetMyOrdersUseCase(repo) }
    val updateUsecase = remember { UpdateOrderStatusUseCase(updaterepo) }

    val ordersVm: MyOrdersViewModel = viewModel(factory = MyOrdersViewModelFactory(getUsecase))
    val context = LocalContext.current

    val userStore  = remember { SecureUserStore(context) }
    val updateVm: UpdateOrderStatusViewModel =
        viewModel(
            factory =
                UpdateOrderStatusViewModelFactory(
                    updateUsecase,
                    userStore
                ),
        )
    val repoUsers = remember { UsersRepositoryImpl(RetrofitProvider.usersApi) }
    val getUsers = remember { GetActiveUsersUseCase(repoUsers) }
    val agentsVm: ActiveAgentsViewModel = viewModel(factory = ActiveAgentsViewModelFactory(getUsers))
    val agentsState by agentsVm.state.collectAsState()

    var reassignOrderId by remember { mutableStateOf<String?>(null) }

    // --- pass a lambda to open sheet from card:
    val onReassignRequested: (String) -> Unit = { orderId ->
        reassignOrderId = orderId
        agentsVm.load()
    }
    val state by ordersVm.state.collectAsState()
    val updatingIds by updateVm.updatingIds.collectAsState()
    val ctx = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()

    ordersEffects(ordersVm, state, listState, snack, ctx)

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

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = { bottomStickyButton(text = stringResource(R.string.order_pool)) {} },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { ordersVm.refresh(ctx) },
            state = pullState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            ordersContent(
                ordersVm = ordersVm,
                updateVm = updateVm,
                state = state,
                listState = listState,
                onOpenOrderDetails = onOpenOrderDetails,
                context = ctx,
                updatingIds = updatingIds,
                onReassignRequested = onReassignRequested,
            )
        }
    }
    reassignBottomSheet(
        open = reassignOrderId != null,
        state = agentsState,
        onDismiss = { reassignOrderId = null },
        onRetry = { agentsVm.load() },
        onSelect = { user ->
            val orderId = reassignOrderId ?: return@reassignBottomSheet
            OrderLogger.uiTap(orderId, state.orders.firstOrNull { it.id == orderId }?.orderNumber, "Menu:Reassign→${user.name}")
            updateVm.update(orderId, OrderStatus.REASSIGNED, assignedAgentId = user.id)
            reassignOrderId = null
        },
    )
}

@Composable
private fun ordersEffects(
    vm: MyOrdersViewModel,
    state: MyOrdersUiState,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    context: Context,
) {
    LaunchedEffect(Unit) { vm.loadOrders(context) }

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
        if (shouldLoadMore && !state.isRefreshing && !state.isLoading) vm.loadNextPage(context)
    }
}

// ==============================
// ordersContent (screen snippet)
// ==============================
@Composable
private fun ordersContent(
    ordersVm: MyOrdersViewModel,
    updateVm: UpdateOrderStatusViewModel,
    state: MyOrdersUiState,
    listState: LazyListState,
    onOpenOrderDetails: (String) -> Unit,
    context: android.content.Context,
    updatingIds: Set<String>,
    onReassignRequested: (String) -> Unit,
) {
    // Collect once to avoid multiple recompositions in params
    val uiState by ordersVm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.orders.isEmpty() -> loadingView()
            state.errorMessage != null -> errorView(state.errorMessage!!) { ordersVm.retry(context) }
            state.emptyMessage != null -> emptyView(state.emptyMessage!!)
            else -> orderList(
                state = OrderListState(
                    orders = uiState.orders,
                    listState = listState,
                    isLoadingMore = uiState.isLoadingMore,
                    updatingIds = updatingIds,
                ),
                updateVm = updateVm,
                callbacks = OrderListCallbacks(
                    onDetails = onOpenOrderDetails,
                    onReassignRequested = onReassignRequested,
                    onCall = { id ->
                        val order = uiState.orders.firstOrNull { it.id == id }
                        val phone = order?.customerPhone
                        if (!phone.isNullOrBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        } else {
                            LocalUiOnlyStatusBus.errorEvents.tryEmit(
                                context.getString(R.string.phone_missing) to null
                            )
                        }
                    },
                    onAction = { orderId, dialog ->
                        val order = uiState.orders.firstOrNull { it.id == orderId }
                        val label = when (dialog) {
                            ActionDialog.Confirm -> "Confirm"
                            ActionDialog.PickUp  -> "PickUp"
                            ActionDialog.Start   -> "StartDelivery"
                            ActionDialog.Deliver -> "Deliver"
                            ActionDialog.Fail    -> "DeliveryFailed"
                        }
                        UpdateOrderStatusViewModel.OrderLogger.uiTap(orderId, order?.orderNumber, label)

                        when (dialog) {
                            ActionDialog.Confirm -> updateVm.update(orderId, OrderStatus.CONFIRMED)
                            ActionDialog.PickUp  -> updateVm.update(orderId, OrderStatus.PICKUP)
                            ActionDialog.Start   -> updateVm.update(orderId, OrderStatus.START_DELIVERY)
                            ActionDialog.Deliver -> updateVm.update(orderId, OrderStatus.DELIVERY_DONE)
                            ActionDialog.Fail    -> updateVm.update(orderId, OrderStatus.DELIVERY_FAILED)
                        }
                    },
                    onRefresh = { ordersVm.refreshOrders() } // IMPORTANT: real reload
                )
            )
        }
    }
}

@Composable
fun myOrderCard(
    order: OrderInfo,
    isUpdating: Boolean,
    onDetails: () -> Unit,
    onCall: () -> Unit,
    onAction: (ActionDialog) -> Unit,
    onReassignRequested: () -> Unit,
    updateVm: UpdateOrderStatusViewModel,
) {
    var dialog by remember { mutableStateOf<ActionDialog?>(null) }
    var showReassign by remember { mutableStateOf(false) }
    val reassignLabel = stringResource(R.string.reassign_order)
    Card(
        modifier = Modifier.fillMaxWidth(), // ← restore
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(dimensionResource(R.dimen.largeSpace))) {
            orderHeaderWithMenu(
                order = order,
                enabled = !isUpdating,
                onPickUp = {
                    updateVm.update(order.id, OrderStatus.PICKUP)
                },
                onCancel = {
                    updateVm.update(order.id, OrderStatus.CANCELED)
                },
                onReassign = { onReassignRequested() },
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.mediumSpace)))

            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace))) {
                OutlinedButton(
                    onClick = onDetails,
                    enabled = !isUpdating,
                    modifier = Modifier.weight(0.8f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
                ) {
                    Spacer(Modifier.width(dimensionResource(R.dimen.smallerSpace)))
                    Text(text = stringResource(R.string.order_details), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                }

                when (order.status) {
                    OrderStatus.ADDED ->
                        primaryActionButton(
                            text = stringResource(R.string.confirm_order),
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdating,
                            onClick = { dialog = ActionDialog.Confirm },
                        )
                    OrderStatus.CONFIRMED ->
                        primaryActionButton(
                            text = stringResource(R.string.pick_order),
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdating,
                            onClick = { dialog = ActionDialog.PickUp },
                        )
                    OrderStatus.PICKUP ->
                        primaryActionButton(
                            text = stringResource(R.string.start_delivery),
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdating,
                            onClick = { dialog = ActionDialog.Start },
                        )
                    OrderStatus.START_DELIVERY ->
                        primaryActionButton(
                            text = stringResource(R.string.deliver_order),
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdating,
                            onClick = { dialog = ActionDialog.Deliver },
                        )
                    else -> {}
                }
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))

            if (order.status == OrderStatus.PICKUP || order.status == OrderStatus.START_DELIVERY) {
                OutlinedButton(
                    onClick = { dialog = ActionDialog.Fail },
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
                ) { Text(stringResource(R.string.delivery_failed)) }
            }

            callButton(onCall)
        }
    }

    if (showReassign) {
        reassignDialog(
            onDismiss = { showReassign = false },
            onConfirm = { assignee ->
                LocalUiOnlyStatusBus.errorEvents.tryEmit(
                    "$reassignLabel → $assignee" to null,
                )
                showReassign = false
            },
        )
    }

    when (dialog) {
        ActionDialog.Confirm ->
            simpleConfirmDialog(
                title = stringResource(R.string.confirm_order),
                onDismiss = { dialog = null },
                onConfirm = {
                    onAction(ActionDialog.Confirm)
                    dialog = null
                },
            )
        ActionDialog.PickUp ->
            simpleConfirmDialog(
                title = stringResource(R.string.pick_order),
                onDismiss = { dialog = null },
                onConfirm = {
                    onAction(ActionDialog.PickUp)
                    dialog = null
                },
            )
        ActionDialog.Start ->
            simpleConfirmDialog(
                title = stringResource(R.string.start_delivery),
                onDismiss = { dialog = null },
                onConfirm = {
                    onAction(ActionDialog.Start)
                    dialog = null
                },
            )
        ActionDialog.Deliver ->
            deliverDialog(
                onDismiss = { dialog = null },
                onConfirm = {
                    onAction(ActionDialog.Deliver)
                    dialog = null
                },
            )
        ActionDialog.Fail ->
            reasonDialog(
                title = stringResource(R.string.delivery_failed),
                onDismiss = { dialog = null },
                onConfirm = {
                    onAction(ActionDialog.Fail)
                    dialog = null
                },
            )
        null -> {}
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
            Text(text = stringResource(id = R.string.order_details), style = MaterialTheme.typography.titleSmall, maxLines = 1)
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
    if (status.equals("confirmed", ignoreCase = true) ||
        status.equals("added", ignoreCase = true)
    ) {
        SuccessGreen
    } else {
        MaterialTheme.colorScheme.onSurface
    }
