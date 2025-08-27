package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MarkerState
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.components.customBottom
import com.ntg.lmd.mainscreen.ui.components.mapCenter
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
                customBottom(
                    orders = ui.orders,
                    selectedOrderNumber = ui.selectedOrderNumber,
                    onOrderClick = { order -> focusOnOrder(order, false) },
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
