package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MarkerState
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.components.customBottom
import com.ntg.lmd.mainscreen.ui.components.mapCenter
import com.ntg.lmd.mainscreen.ui.model.MapStates
import com.ntg.lmd.mainscreen.ui.screens.orders.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.screens.orders.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.screens.orders.model.OrderUI
import com.ntg.lmd.mainscreen.ui.viewmodel.MyPoolVMFactory
import com.ntg.lmd.mainscreen.ui.viewmodel.MyPoolViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val INITIAL_CAMERA_ZOOM = 14f
private const val MY_ORDER_FOCUS_ZOOM = 15f
private val PAGING_SPINNER_BOTTOM_PADDING = 12.dp
private val ZERO_LATLNG = LatLng(0.0, 0.0)

private fun OrderInfo.hasValidLatLng(): Boolean = lat.isFinite() && lng.isFinite() && !(lat == 0.0 && lng == 0.0)
// --- OrderInfo -> OrderUI mapper (adapt field names if yours differ) ---
private fun OrderInfo.toOrderUI(): OrderUI =
    OrderUI(
        // OrderUI expects Long
        orderNumber = orderNumber,
        status = status.name.lowercase(),              // matches your statusEnum mapping
        customerName = name ,           // fallback to `name`
        customerPhone = null,
        totalPrice = price,
        details =null,
        distanceMeters = distanceKm
    )

@Composable
fun myPoolScreen(
    viewModel: MyPoolViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(
            factory = MyPoolVMFactory(),
        ),
) {
    val ui by viewModel.ui.collectAsState()

    val mapStates =
        remember {
            MapStates(
                CameraPositionState(),
                MarkerState(ZERO_LATLNG),
            )
        }
    val scope = rememberCoroutineScope()
    val focusOnOrder =
        rememberFocusOnMyOrder(
            viewModel,
            mapStates.markerState,
            mapStates.cameraPositionState,
            scope,
        )

    // Initial zoom to first valid order
    LaunchedEffect(ui.orders) {
        ui.orders.firstOrNull { it.hasValidLatLng() }?.let { first ->
            val target = LatLng(first.lat, first.lng)
            mapStates.markerState.position = target
            mapStates.cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(
                    target,
                    INITIAL_CAMERA_ZOOM,
                ),
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Map background
        mapCenter(
            ui = ui,
            mapStates = mapStates,
            deviceLatLng = ZERO_LATLNG,
        )

        // Bottom carousel
        if (ui.orders.isNotEmpty()) {
            Column(Modifier.align(Alignment.BottomCenter)) {
                myPoolBottom(
                    orders = ui.orders,
                    selectedOrderNumber = ui.selectedOrderNumber,

                    onCenteredOrderChange = { order, index ->
                        focusOnOrder(order, false)
                        viewModel.onCenteredOrderChange(order, index)
                    },
                )

                // Paging loader
                AnimatedVisibility(visible = ui.isLoadingMore) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = PAGING_SPINNER_BOTTOM_PADDING),
                    )
                }
            }
        }

        // Initial loader
        if (ui.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
fun rememberFocusOnMyOrder(
    viewModel: MyPoolViewModel,
    markerState: MarkerState,
    cameraPositionState: CameraPositionState,
    scope: CoroutineScope,
    focusZoom: Float = MY_ORDER_FOCUS_ZOOM,
): (OrderInfo, Boolean) -> Unit {
    val vm = rememberUpdatedState(viewModel)
    val marker = rememberUpdatedState(markerState)
    val camera = rememberUpdatedState(cameraPositionState)
    val coroutineScope = rememberUpdatedState(scope)

    return remember {
        { order: OrderInfo, _: Boolean ->
            vm.value.onCenteredOrderChange(order)

            // update marker position
            marker.value.position = LatLng(order.lat, order.lng)

            // animate camera zoom
            coroutineScope.value.launch {
                camera.value.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(order.lat, order.lng),
                        focusZoom,
                    ),
                )
            }
        }
    }
}

@Composable
fun myPoolBottom(
    orders: List<OrderInfo>,
    selectedOrderNumber: String?,
    onCenteredOrderChange: (OrderInfo, Int) -> Unit = { _, _ -> },
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val sidePadding =
        ((LocalConfiguration.current.screenWidthDp.dp - dimensionResource(id = R.dimen.orders_card_width)) / 2)
            .coerceAtLeast(0.dp)
    val px = with(LocalDensity.current) { sidePadding.roundToPx() }

    var lastCentered by remember { mutableIntStateOf(-1) }
    var programmatic by remember { mutableStateOf(false) }

    // Jump to externally selected order
    LaunchedEffect(selectedOrderNumber, orders) {
        if (!selectedOrderNumber.isNullOrEmpty()) {
            val i = orders.indexOfFirst { it.orderNumber == selectedOrderNumber }
            if (i >= 0) {
                programmatic = true
                try { listState.animateScrollToItem(i, -px) } finally { programmatic = false }
                lastCentered = i
                onCenteredOrderChange(orders[i], i)
            }
        }
    }

    // Notify when a new card becomes centered
    LaunchedEffect(orders, listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { moving ->
            if (!moving && !programmatic && orders.isNotEmpty()) {
                val info = listState.layoutInfo
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                val nearest = info.visibleItemsInfo.minByOrNull {
                    kotlin.math.abs((it.offset + it.size / 2) - center)
                } ?: return@collect
                val idx = nearest.index
                if (idx in orders.indices && idx != lastCentered) {
                    lastCentered = idx
                    onCenteredOrderChange(orders[idx], idx)
                }
            }
        }
    }

    if (orders.isEmpty()) return

    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.primary),
    ) {
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace)),
            contentPadding = PaddingValues(start = sidePadding, end = sidePadding),
        ) {
            itemsIndexed(orders, key = { _, order -> order.orderNumber }) { index, info ->
                // Map once per item and remember
                val uiOrder = remember(info) { info.toOrderUI() }

                myOrderorderCard(
                    order = uiOrder,
                    onDetails = {},
                    onConfirmOrPick = { /* not used inside the card body; keep if needed later */ },
                    onCall = {},
                )
            }
        }
    }
}