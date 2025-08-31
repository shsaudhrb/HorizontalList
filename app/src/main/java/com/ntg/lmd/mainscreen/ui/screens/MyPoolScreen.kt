package com.ntg.lmd.mainscreen.ui.screens

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MarkerState
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.components.mapCenter
import com.ntg.lmd.mainscreen.ui.components.myPoolBottom
import com.ntg.lmd.mainscreen.ui.model.MapStates
import com.ntg.lmd.mainscreen.ui.viewmodel.MyPoolVMFactory
import com.ntg.lmd.mainscreen.ui.viewmodel.MyPoolViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val INITIAL_CAMERA_ZOOM = 14f
private const val MY_ORDER_FOCUS_ZOOM = 15f
private val PAGING_SPINNER_BOTTOM_PADDING = 12.dp
private val ZERO_LATLNG = LatLng(0.0, 0.0)

private fun OrderInfo.hasValidLatLng(): Boolean = lat.isFinite() && lng.isFinite() && !(lat == 0.0 && lng == 0.0)

@Composable
fun myPoolScreen(
    viewModel: MyPoolViewModel = viewModel(factory = MyPoolVMFactory()),
    onOpenOrderDetails: (String) -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current

    var bottomBarHeight by remember { mutableStateOf(0.dp) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fused =
                com.google.android.gms.location.LocationServices
                    .getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                viewModel.updateDeviceLocation(loc)
            }
        }
    }

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
            viewModel = viewModel,
            markerState = mapStates.markerState,
            cameraPositionState = mapStates.cameraPositionState,
            scope = scope,
        )
    var didInitialCameraPosition by remember { mutableStateOf(false) }

    LaunchedEffect(ui.orders.isNotEmpty(), ui.selectedOrderNumber) {
        if (!didInitialCameraPosition && ui.orders.isNotEmpty() && ui.selectedOrderNumber == null) {
            val first = ui.orders.firstOrNull { it.hasValidLatLng() } ?: return@LaunchedEffect
            didInitialCameraPosition = true

            val target = LatLng(first.lat, first.lng)
            mapStates.markerState.position = target
            mapStates.cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(target, INITIAL_CAMERA_ZOOM),
            )
        }
    }
    Box(Modifier.fillMaxSize()) {
        mapCenter(
            ui = ui,
            mapStates = mapStates,
            deviceLatLng = ZERO_LATLNG,
            bottomOverlayPadding = bottomBarHeight + dimensionResource(id = R.dimen.largeSpace), // small extra gap
        )

        if (ui.orders.isNotEmpty()) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .onGloballyPositioned { layout ->
                        bottomBarHeight = with(density) { layout.size.height.toDp() }
                    },
            ) {
                myPoolBottom(
                    orders = ui.orders,
                    onCenteredOrderChange = { order, index ->
                        focusOnOrder(order, false)
                        viewModel.onCenteredOrderChange(order, index)
                    },
                    onOpenOrderDetails = onOpenOrderDetails,
                    onNearEnd = { idx -> viewModel.loadNextIfNeeded(idx) },
                )

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

        if (ui.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
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

            if (order.hasValidLatLng()) {
                val target = LatLng(order.lat, order.lng)
                marker.value.position = target
                coroutineScope.value.launch {
                    camera.value.animate(
                        CameraUpdateFactory.newLatLngZoom(target, focusZoom),
                    )
                }
            }
        }
    }
}
