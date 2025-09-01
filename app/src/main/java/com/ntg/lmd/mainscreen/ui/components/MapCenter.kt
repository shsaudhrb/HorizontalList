package com.ntg.lmd.mainscreen.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
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

private data class MapChrome(
    val top: Dp,
    val bottom: Dp,
)

private data class MapConfig(
    val ui: MapUiState,
    val mapStates: MapStates,
    val deviceLatLng: LatLng?,
    val canShowMyLocation: Boolean,
)

@Composable
fun mapCenter(
    ui: MapUiState,
    mapStates: MapStates,
    deviceLatLng: LatLng?,
    modifier: Modifier = Modifier,
    bottomOverlayPadding: Dp? = null,
) {
    val (cameraPositionState, _) = mapStates
    val (topDefault, bottomDefault) = overlayHeights()
    val chrome =
        remember(topDefault, bottomDefault, bottomOverlayPadding) {
            MapChrome(top = topDefault, bottom = bottomOverlayPadding ?: bottomDefault)
        }
    val canShowMyLocation = rememberCanShowMyLocation()

    var initialCentered by remember { mutableStateOf(false) }
    initialCenterEffect(deviceLatLng, cameraPositionState, initialCentered) { initialCentered = it }

    val config =
        remember(ui, mapStates, deviceLatLng, canShowMyLocation) {
            MapConfig(ui, mapStates, deviceLatLng, canShowMyLocation)
        }

    googleMapContent(config = config, chrome = chrome, modifier = modifier)
}

// ---------- helpers (all ≤5 params) ----------

@Composable
private fun initialCenterEffect(
    deviceLatLng: LatLng?,
    camera: CameraPositionState,
    alreadyCentered: Boolean,
    setCentered: (Boolean) -> Unit,
) {
    LaunchedEffect(deviceLatLng, alreadyCentered) {
        if (deviceLatLng != null && !alreadyCentered) {
            camera.animate(CameraUpdateFactory.newLatLngZoom(deviceLatLng, 8f))
            setCentered(true)
        }
    }
}

@Composable
private fun googleMapContent(
    config: MapConfig,
    chrome: MapChrome,
    modifier: Modifier = Modifier,
) {
    val (cameraPositionState, markerState) = config.mapStates
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = config.canShowMyLocation),
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
        contentPadding = PaddingValues(top = chrome.top, bottom = chrome.bottom),
    ) {
        distanceCircle(
            deviceLatLng = config.deviceLatLng,
            distanceKm = config.ui.distanceThresholdKm,
        )
        otherMarkers(
            orders = config.ui.mapOrders,
            selectedOrderNumber = config.ui.selected?.orderNumber,
        )
        selectedMarkerPositionEffect(selected = config.ui.selected, markerState = markerState)
        selectedMarker(
            selected = config.ui.selected,
            hasLocationPerm = config.ui.hasLocationPerm,
            thresholdKm = config.ui.distanceThresholdKm,
            markerState = markerState,
        )
    }
}

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
