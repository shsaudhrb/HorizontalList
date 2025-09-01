package com.ntg.lmd.mainscreen.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MarkerState
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.model.MapStates
import com.ntg.lmd.mainscreen.ui.viewmodel.MyPoolViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val INITIAL_CAMERA_ZOOM = 14f
private const val MY_ORDER_FOCUS_ZOOM = 15f

private fun OrderInfo.hasValidLatLng(): Boolean = lat.isFinite() && lng.isFinite() && !(lat == 0.0 && lng == 0.0)

@Composable
fun locationPermissionAndLastLocation(viewModel: MyPoolViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val fused =
                LocationServices
                    .getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc -> viewModel.updateDeviceLocation(loc) }
        }
    }
}

@Composable
fun initialCameraPositionEffect(
    orders: List<OrderInfo>,
    selectedOrderNumber: String?,
    mapStates: MapStates,
) {
    var didInitialCamera by remember { mutableStateOf(false) }
    LaunchedEffect(orders.isNotEmpty(), selectedOrderNumber) {
        if (!didInitialCamera && orders.isNotEmpty() && selectedOrderNumber == null) {
            val first = orders.firstOrNull { it.hasValidLatLng() } ?: return@LaunchedEffect
            didInitialCamera = true
            val target = LatLng(first.lat, first.lng)
            mapStates.markerState.position = target
            mapStates.cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(target, INITIAL_CAMERA_ZOOM),
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
            if (order.hasValidLatLng()) {
                val target = LatLng(order.lat, order.lng)
                marker.value.position = target
                coroutineScope.value.launch {
                    camera.value.animate(CameraUpdateFactory.newLatLngZoom(target, focusZoom))
                }
            }
        }
    }
}
