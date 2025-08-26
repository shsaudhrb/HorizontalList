package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                cameraPositionState = CameraPositionState(),
                markerState = MarkerState(LatLng(30.0444, 31.2357)), // default Cairo
            )
        }

    val scope = rememberCoroutineScope()

    // 👇 get the focusOnOrder lambda from rememberFocusOnOrder
    val focusOnOrder =
        rememberFocusOnMyOrder(
            viewModel = viewModel,
            markerState = mapStates.markerState,
            cameraPositionState = mapStates.cameraPositionState,
            scope = scope,
        )

    Box(Modifier.fillMaxSize()) {
        // top: map
        mapCenter(
            ui = ui,
            mapStates = mapStates,
            deviceLatLng = LatLng(30.0444, 31.2357),
        )

        // bottom: carousel
        Box(Modifier.align(Alignment.BottomCenter)) {
            customBottom(
                orders = ui.orders,
                selectedOrderNumber = ui.selectedOrderNumber,
                onOrderClick = { order ->
                    focusOnOrder(order, false) // center map on tapped card
                },
                onCenteredOrderChange = { order, _ ->
                    focusOnOrder(order, false) // center map on centered card
                },
            )
        }

        if (ui.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

// we will use this focusOnOrder when we search orders and click on it to be showing on the map
@Composable
fun rememberFocusOnMyOrder(
    viewModel: MyPoolViewModel,
    markerState: MarkerState,
    cameraPositionState: CameraPositionState,
    scope: CoroutineScope,
    focusZoom: Float = ORDER_FOCUS_ZOOM,
): (OrderInfo, Boolean) -> Unit {
    // keep latest references without re-allocating the lambda on every recomposition
    val vm = rememberUpdatedState(viewModel)
    val marker = rememberUpdatedState(markerState)
    val camera = rememberUpdatedState(cameraPositionState)
    val coroutineScope = rememberUpdatedState(scope)

    return remember {
        { order: OrderInfo, _: Boolean ->
            // update ViewModel
            vm.value.onCenteredOrderChange(order, 0) // or pass index if you track it

            // move the marker on the map to that order's location
            marker.value.position = LatLng(order.lat, order.lng)

            // animate the map camera to zoom in on the order
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
