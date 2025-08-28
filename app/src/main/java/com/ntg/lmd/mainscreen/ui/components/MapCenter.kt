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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.ntg.lmd.mainscreen.domain.model.GeneralPoolUiState
import com.ntg.lmd.mainscreen.domain.model.MapStates

// screen height
private const val TOP_OVERLAY_RATIO = 0.09f // 9% of screen height
private const val BOTTOM_BAR_RATIO = 0.22f // 22% of screen height

@Composable
fun mapCenter(
    ui: GeneralPoolUiState,
    mapStates: MapStates,
    deviceLatLng: LatLng?,
    modifier: Modifier = Modifier,
) {
    val (cameraPositionState, markerState) = mapStates

    // Screen-aware padding so map UI (incl. +/- and my-location button) isn't hidden
    val cfg = LocalConfiguration.current
    val screenH = cfg.screenHeightDp.dp

    val topOverlayHeight = (screenH * TOP_OVERLAY_RATIO).coerceIn(48.dp, 96.dp)
    val bottomBarHeight = (screenH * BOTTOM_BAR_RATIO).coerceIn(128.dp, 280.dp)

    // Enable blue dot only if we have location permission (prevents SecurityException)
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
    val canShowMyLocation = hasFine || hasCoarse

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        // Blue dot + target button
        properties = MapProperties(isMyLocationEnabled = canShowMyLocation),
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
        contentPadding = PaddingValues(top = topOverlayHeight, bottom = bottomBarHeight),
    ) {
        // Filter radius circle
        if (deviceLatLng != null && ui.distanceThresholdKm > 0.0) {
            Circle(
                center = deviceLatLng,
                radius = ui.distanceThresholdKm * 1000.0, // km -> meters
                strokeWidth = 3f,
                strokeColor = MaterialTheme.colorScheme.primary,
                fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                zIndex = 0.5f,
            )
        }

        // Markers for every order (except the selected one; it has its own marker)
        val selectedOrderNumber = ui.selected?.orderNumber
        ui.mapOrders.forEach { order ->
            if (order.orderNumber != selectedOrderNumber) {
                val position =
                    remember(order.lat, order.lng) {
                        LatLng(order.lat, order.lng)
                    }
                Marker(
                    state = remember { MarkerState(position) },
                    title = order.name,
                    snippet = order.orderNumber,
                    zIndex = 0f,
                )
            }
        }

        // keep the marker pinned to the currently selected order
        LaunchedEffect(ui.selected?.lat, ui.selected?.lng) {
            ui.selected?.let { sel ->
                markerState.position = LatLng(sel.lat, sel.lng)
            }
        }

        // render the single marker only if we have a selection (special/high zIndex)
        ui.selected?.let { sel ->
            val withinRange =
                !ui.hasLocationPerm || (sel.distanceKm.isFinite() && sel.distanceKm <= ui.distanceThresholdKm)

            if (withinRange) {
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
