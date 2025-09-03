package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MarkerState
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.components.HorizontalListCallbacks
import com.ntg.lmd.mainscreen.ui.components.generalHorizontalList
import com.ntg.lmd.mainscreen.ui.components.initialCameraPositionEffect
import com.ntg.lmd.mainscreen.ui.components.locationPermissionAndLastLocation
import com.ntg.lmd.mainscreen.ui.components.mapCenter
import com.ntg.lmd.mainscreen.ui.components.myPoolOrderCardItem
import com.ntg.lmd.mainscreen.ui.components.rememberFocusOnMyOrder
import com.ntg.lmd.mainscreen.ui.model.MapStates
import com.ntg.lmd.mainscreen.ui.model.MapUiState
import com.ntg.lmd.mainscreen.ui.model.MyOrdersPoolUiState
import com.ntg.lmd.mainscreen.ui.viewmodel.MyPoolVMFactory
import com.ntg.lmd.mainscreen.ui.viewmodel.MyPoolViewModel

private val ZERO_LATLNG = LatLng(0.0, 0.0)

private data class MapOverlayState(
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val orders: List<OrderInfo>,
    val bottomPadding: Dp,
    val mapUi: MapUiState,
    val mapStates: MapStates,
)

private data class MapOverlayCallbacks(
    val onBottomHeightMeasured: (Int) -> Unit,
    val onCenteredOrderChange: (OrderInfo, Int) -> Unit,
    val onOpenOrderDetails: (String) -> Unit,
    val onNearEnd: (Int) -> Unit,
)

@Composable
fun rememberMapStates(): MapStates = remember { MapStates(CameraPositionState(), MarkerState(ZERO_LATLNG)) }

@Composable
fun myPoolScreen(
    viewModel: MyPoolViewModel = viewModel(factory = MyPoolVMFactory()),
    onOpenOrderDetails: (String) -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    var bottomBarHeight by remember { mutableStateOf(0.dp) }

    locationPermissionAndLastLocation(viewModel)
    val mapStates = rememberMapStates()
    initialCameraPositionEffect(ui.orders, ui.selectedOrderNumber, mapStates)

    val state = overlayState(ui, bottomBarHeight, mapStates)
    val callbacks =
        rememberOverlayCallbacks(
            viewModel = viewModel,
            mapStates = mapStates,
            onOpenOrderDetails = onOpenOrderDetails,
            onNearEnd = { idx -> viewModel.loadNextIfNeeded(idx) },
            setBottomBarHeight = { bottomBarHeight = it },
        )

    mapWithBottomOverlay(state = state, callbacks = callbacks)
}

@Composable
private fun overlayState(
    ui: MyOrdersPoolUiState,
    bottomBarHeight: Dp,
    mapStates: MapStates,
): MapOverlayState {
    val extra = dimensionResource(R.dimen.largeSpace)
    return MapOverlayState(
        isLoading = ui.isLoading,
        isLoadingMore = ui.isLoadingMore,
        orders = ui.orders,
        bottomPadding = bottomBarHeight + extra,
        mapUi = ui,
        mapStates = mapStates,
    )
}

@Composable
private fun rememberOverlayCallbacks(
    viewModel: MyPoolViewModel,
    mapStates: MapStates,
    onOpenOrderDetails: (String) -> Unit,
    onNearEnd: (Int) -> Unit,
    setBottomBarHeight: (Dp) -> Unit,
): MapOverlayCallbacks {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val focus =
        rememberFocusOnMyOrder(
            viewModel = viewModel,
            markerState = mapStates.markerState,
            cameraPositionState = mapStates.cameraPositionState,
            scope = scope,
        )
    return MapOverlayCallbacks(
        onBottomHeightMeasured = { px -> setBottomBarHeight(with(density) { px.toDp() }) },
        onCenteredOrderChange = { order, index ->
            focus(order, false)
            viewModel.onCenteredOrderChange(order, index)
        },
        onOpenOrderDetails = onOpenOrderDetails,
        onNearEnd = onNearEnd,
    )
}

@Composable
private fun mapWithBottomOverlay(
    state: MapOverlayState,
    callbacks: MapOverlayCallbacks,
) {
    Box(Modifier.fillMaxSize()) {
        mapCenter(
            ui = state.mapUi,
            mapStates = state.mapStates,
            deviceLatLng = ZERO_LATLNG,
            bottomOverlayPadding = state.bottomPadding,
        )
        if (state.orders.isNotEmpty()) {
            bottomOverlay(state, callbacks) // now a BoxScope extension
        }
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun BoxScope.bottomOverlay(
    state: MapOverlayState,
    callbacks: MapOverlayCallbacks,
) {
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .onGloballyPositioned { callbacks.onBottomHeightMeasured(it.size.height) },
    ) {
        loadingMoreIndicator(state)
        ordersHorizontalList(state, callbacks)
    }
}

@Composable
private fun loadingMoreIndicator(state: MapOverlayState) {
    AnimatedVisibility(visible = state.isLoadingMore) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = dimensionResource(R.dimen.smallSpace)),
            )
        }
    }
}

@Composable
private fun ordersHorizontalList(
    state: MapOverlayState,
    callbacks: MapOverlayCallbacks,
) {
    generalHorizontalList(
        orders = state.orders,
        selectedOrderNumber = state.mapUi.selected?.orderNumber,
        callbacks =
            HorizontalListCallbacks(
                onCenteredOrderChange = { order, index ->
                    callbacks.onCenteredOrderChange(order, index)
                },
                onNearEnd = { idx -> callbacks.onNearEnd(idx) },
            ),
        cardContent = { order, _ ->
            myPoolOrderCardItem(
                order = order,
                onOpenOrderDetails = callbacks.onOpenOrderDetails,
                onCall = { },
            )
        },
    )
}
