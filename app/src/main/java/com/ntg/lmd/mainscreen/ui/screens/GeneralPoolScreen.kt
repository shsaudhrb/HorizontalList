package com.ntg.lmd.mainscreen.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.components.customBottom
import com.ntg.lmd.mainscreen.ui.components.customHeader
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun generalPoolScreen(
    navController: NavController,
    generalPoolViewModel: GeneralPoolViewModel = viewModel(),
) {
    val context = LocalContext.current
    val ui by generalPoolViewModel.ui.collectAsStateWithLifecycle()
    val madinaCenter = LatLng(24.5247, 39.5692)

    // selected order and camera position to it
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(madinaCenter, 12f)
        }
    val markerState = remember { MarkerState(position = madinaCenter) }
    val scope = rememberCoroutineScope()

    // Load Local orders.json from assets
    LaunchedEffect(Unit) {
        generalPoolViewModel.loadOrdersFromAssets(context)
    }

    // Ask for location permission
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            val granted =
                (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                        (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
            generalPoolViewModel.onPermissionsResult(granted)
            if (granted) {
                generalPoolViewModel.fetchAndApplyDistances(context) // returns Unit
            }
        }

    LaunchedEffect(Unit) {
        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED

        generalPoolViewModel.onPermissionsResult(granted)
        if (granted) {
            generalPoolViewModel.fetchAndApplyDistances(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    Scaffold(
        topBar = {
            // ---- custom header with search bar ----
            customHeader(
                title = "General Pool",
                showBackIcon = true,
                onBackClick = { navController.popBackStack() },
                showEndIcon = true,
                modifier = Modifier.statusBarsPadding(),
                searching = ui.searching,
                onSearchingChange = generalPoolViewModel::onSearchingChange,
                query = ui.query,
                onQueryChange = generalPoolViewModel::onQueryChange,
            )
        },
        // ---- if there's orders, the custom bottom will be shown, if not, it won't be showing ----
        bottomBar = {
            if (ui.isLoading) {
                Text(
                    text = "Loading orders…",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else if (ui.mapOrders.isNotEmpty()) {
                customBottom(
                    orders = ui.mapOrders,
                    onOrderClick = { order ->
                        // select + animate camera to order that we selected
                        generalPoolViewModel.onOrderSelected(order)
                        markerState.position = LatLng(order.lat, order.lng)
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(order.lat, order.lng),
                                    14f
                                ),
                            )
                        }
                    },
                    onCenteredOrderChange = { order, _ ->
                        generalPoolViewModel.onOrderSelected(order)
                        markerState.position = LatLng(order.lat, order.lng)
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(order.lat, order.lng),
                                    14f
                                ),
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // animated dropdown list for search results
            AnimatedVisibility(
                visible = ui.searching && ui.query.isNotBlank() && ui.filteredOrders.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    tonalElevation = 2.dp,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                    ) {
                        ui.filteredOrders.forEach { order ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // pick order from search
                                            generalPoolViewModel.onOrderSelected(order)
                                            markerState.position = LatLng(order.lat, order.lng)
                                            scope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        LatLng(order.lat, order.lng),
                                                        14f,
                                                    ),
                                                )
                                            }
                                            generalPoolViewModel.onSearchingChange(false)
                                            generalPoolViewModel.onQueryChange("")
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = "${order.orderNumber} • ${order.name}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                }
            }

            // distance filter slider
            distanceFilterBar(
                maxDistanceKm = ui.distanceThresholdKm,
                onMaxDistanceKm = generalPoolViewModel::onDistanceChange,
                enabled = ui.hasLocationPerm,
            )

            // map section with circles and markers
            mapCenter(
                selected = ui.selected,
                orders = ui.mapOrders,
                cameraPositionState = cameraPositionState,
                markerState = markerState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
private fun mapCenter(
    selected: OrderInfo?,
    orders: List<OrderInfo>,
    modifier: Modifier = Modifier,
    markerState: MarkerState,
    cameraPositionState: CameraPositionState,
) {
    // for zoom in & zoom out
    val zoom = cameraPositionState.position.zoom

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
    ) {
        orders.forEach { order ->
            val isSelected = selected?.orderNumber == order.orderNumber

            // Scale radius inversely with zoom
            val baseRadius = if (isSelected) 250.0 else 150.0
            val scaledRadius = baseRadius * (20f / zoom.toDouble())

            Circle(
                center = LatLng(order.lat, order.lng),
                radius = scaledRadius,
                strokeWidth = if (isSelected) 4f else 2f,
                strokeColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                    },
                fillColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    },
                zIndex = if (isSelected) 1f else 0f,
            )
        }

        LaunchedEffect(selected?.lat, selected?.lng) {
            selected?.let { sel ->
                markerState.position = LatLng(sel.lat, sel.lng)
            }
        }

        selected?.let {
            Marker(
                state = markerState,
                title = it.name,
                snippet = it.orderNumber,
                zIndex = 1f,
            )
        }
    }
}

// distance filter component
// used to select max distance in km for filtering orders
@Composable
private fun distanceFilterBar(
    maxDistanceKm: Float,
    onMaxDistanceKm: (Float) -> Unit,
    enabled: Boolean,
) {
    // Allowed discrete values (0, 10, 20, …, 100)
    val distanceRangeKm = listOf(0f, 10f, 20f, 30f, 40f, 50f, 60f, 70f, 80f, 90f, 100f)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "Distance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$maxDistanceKm km",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
        }

        // Slider for the distance range in km
        Slider(
            value = distanceRangeKm.indexOf(maxDistanceKm).takeIf { it >= 0 }?.toFloat() ?: 0f,
            onValueChange = { indexFloat ->
                val index = indexFloat.roundToInt().coerceIn(0, distanceRangeKm.lastIndex)
                onMaxDistanceKm(distanceRangeKm[index])
            },
            valueRange = 0f..distanceRangeKm.lastIndex.toFloat(),
            steps = distanceRangeKm.size - 2, // size = 11, exclude min and max, number of distance range = 9
            enabled = enabled,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (!enabled) {
            Text(
                text = "Please allow location access to enable distance filtering.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
