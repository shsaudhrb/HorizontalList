package com.ntg.lmd.mainscreen.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.model.MapStates
import com.ntg.lmd.mainscreen.ui.model.MapUiState

// screen height
private const val TOP_OVERLAY_RATIO = 0.09f // 9% of screen height
private const val BOTTOM_BAR_RATIO = 0.22f // 22% of screen height

@Composable
fun mapCenter(
    ui: MapUiState,
    mapStates: MapStates,
    deviceLatLng: LatLng?,
    modifier: Modifier = Modifier,
) {
    val (cameraPositionState, markerState) = mapStates
    val (topOverlayHeight, bottomBarHeight) = overlayHeights()
    val canShowMyLocation = rememberCanShowMyLocation()

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = canShowMyLocation),
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
        contentPadding = PaddingValues(top = topOverlayHeight, bottom = bottomBarHeight),
    ) {
        distanceCircle(deviceLatLng = deviceLatLng, distanceKm = ui.distanceThresholdKm)

        otherMarkers(
            orders = ui.mapOrders,
            selectedOrderNumber = ui.selected?.orderNumber,
        )

        selectedMarkerPositionEffect(selected = ui.selected, markerState = markerState)

        selectedMarker(
            selected = ui.selected,
            hasLocationPerm = ui.hasLocationPerm,
            thresholdKm = ui.distanceThresholdKm,
            markerState = markerState,
        )
    }
}

// --------------------------- Small helpers ---------------------------

@Composable
private fun overlayHeights(): Pair<Dp, Dp> {
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val top = (screenH * TOP_OVERLAY_RATIO).coerceIn(48.dp, 96.dp)
    val bottom = (screenH * BOTTOM_BAR_RATIO).coerceIn(128.dp, 280.dp)
    return top to bottom
}

@Composable
private fun rememberCanShowMyLocation(): Boolean {
    val context = LocalContext.current
    val hasFine =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    return hasFine || hasCoarse
}

// --------------------------- Map content pieces ---------------------------

@Composable
private fun distanceCircle(
    deviceLatLng: LatLng?,
    distanceKm: Double,
) {
    if (deviceLatLng != null && distanceKm > 0.0) {
        Circle(
            center = deviceLatLng,
            radius = distanceKm * 1000.0,
            strokeWidth = 3f,
            strokeColor = MaterialTheme.colorScheme.primary,
            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            zIndex = 0.5f,
        )
    }
}

@Composable
private fun otherMarkers(
    orders: List<OrderInfo>,
    selectedOrderNumber: String?,
) {
    orders.forEach { order ->
        if (order.orderNumber != selectedOrderNumber) {
            val position = remember(order.lat, order.lng) { LatLng(order.lat, order.lng) }
            Marker(
                state = remember { MarkerState(position) },
                title = order.name,
                snippet = order.orderNumber,
                zIndex = 0f,
            )
        }
    }
}

@Composable
private fun selectedMarkerPositionEffect(
    selected: OrderInfo?,
    markerState: MarkerState,
) {
    LaunchedEffect(selected?.lat, selected?.lng) {
        selected?.let { markerState.position = LatLng(it.lat, it.lng) }
    }
}

@Composable
private fun selectedMarker(
    selected: OrderInfo?,
    hasLocationPerm: Boolean,
    thresholdKm: Double,
    markerState: MarkerState,
) {
    selected ?: return
    val withinRange =
        !hasLocationPerm || (selected.distanceKm.isFinite() && selected.distanceKm <= thresholdKm)

    if (withinRange) {
        Marker(
            state = markerState,
            title = selected.name,
            snippet = selected.orderNumber,
            zIndex = 1f,
        )
    }
}
