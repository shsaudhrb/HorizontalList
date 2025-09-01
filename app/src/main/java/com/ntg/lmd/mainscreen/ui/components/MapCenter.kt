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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.ntg.lmd.mainscreen.ui.model.MapStates
import com.ntg.lmd.mainscreen.ui.model.MapUiState

// screen height
private const val TOP_OVERLAY_RATIO = 0.09f // 9% of screen height
private const val BOTTOM_BAR_RATIO = 0.22f // 22% of screen height

@Composable
fun mapCenter(
    ui: MapUiState, // << works with BOTH states
    mapStates: MapStates,
    deviceLatLng: LatLng?,
    modifier: Modifier = Modifier,
) {
    val (cameraPositionState, markerState) = mapStates
    var initialCentered by remember { mutableStateOf(false) }

    val cfg = LocalConfiguration.current
    val screenH = cfg.screenHeightDp.dp
    val topOverlayHeight = (screenH * TOP_OVERLAY_RATIO).coerceIn(48.dp, 96.dp)
    val bottomBarHeight = (screenH * BOTTOM_BAR_RATIO).coerceIn(128.dp, 280.dp)

    val context = LocalContext.current
    val hasFine =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    val hasCoarse =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    val canShowMyLocation = hasFine || hasCoarse

    LaunchedEffect(deviceLatLng) {
        if (deviceLatLng != null && !initialCentered) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(deviceLatLng, 8f)
            )
            initialCentered = true
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = canShowMyLocation),
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
        contentPadding = PaddingValues(top = topOverlayHeight, bottom = bottomBarHeight),
    ) {
        if (deviceLatLng != null && ui.distanceThresholdKm > 0.0) {
            Circle(
                center = deviceLatLng,
                radius = ui.distanceThresholdKm * 1000.0,
                strokeWidth = 3f,
                strokeColor = MaterialTheme.colorScheme.primary,
                fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                zIndex = 0.5f,
            )
        }

        val selectedOrderNumber = ui.selected?.orderNumber
        ui.mapOrders.forEach { order ->
            if (order.orderNumber != selectedOrderNumber) {
                val position = LatLng(order.lat, order.lng)
                Marker(
                    state = MarkerState(position = position),
                    title = order.name,
                    snippet = order.orderNumber,
                    zIndex = 0f,
                )
            }
        }

        // render the single marker only if we have a selection (special/high zIndex)
        ui.selected?.let { sel ->
            val withinRange =
                !ui.hasLocationPerm || (sel.distanceKm.isFinite() && sel.distanceKm <= ui.distanceThresholdKm)

            if (withinRange) {
                val pos = LatLng(sel.lat, sel.lng)
                markerState.position = pos
                Marker(
                    state = markerState,
                    title = sel.name,
                    snippet = sel.orderNumber,
                    zIndex = 1f,
                )
            }
        }
    }
}
