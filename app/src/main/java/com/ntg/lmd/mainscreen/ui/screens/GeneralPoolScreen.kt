package com.ntg.lmd.mainscreen.ui.screens

import android.Manifest
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
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.unit.dp
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
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.HeaderUiModel
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.SearchController
import com.ntg.lmd.mainscreen.ui.components.customBottom
import com.ntg.lmd.mainscreen.ui.components.customHeader
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolUiEvent
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Map / Camera behavior
private const val INITIAL_MAP_ZOOM = 12f
private const val ORDER_FOCUS_ZOOM = 14f

// Madinah Latitude and Longitude
private const val DEFAULT_CITY_CENTER_LAT = 24.5247
private const val DEFAULT_CITY_CENTER_LNG = 39.5692
private val DEFAULT_CITY_CENTER = LatLng(DEFAULT_CITY_CENTER_LAT, DEFAULT_CITY_CENTER_LNG)

@Composable
fun generalPoolScreen(
    navController: NavController,
    generalPoolViewModel: GeneralPoolViewModel = viewModel(),
) {
    val context = LocalContext.current
    val ui by generalPoolViewModel.ui.collectAsStateWithLifecycle()

    // default camera position
    val madinaCenter = DEFAULT_CITY_CENTER

    // selected order and camera position to it
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(madinaCenter, INITIAL_MAP_ZOOM)
    }
    val markerState = remember { MarkerState(position = madinaCenter) }
    val scope = rememberCoroutineScope()

    // Load Local orders.json from assets
    LaunchedEffect(Unit) { generalPoolViewModel.loadOrdersFromAssets(context) }

    // handle location permission requests
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            // Re-sync without prompting again (prevents permission dialog loops)
            generalPoolViewModel.ensureLocationReady(context, promptIfMissing = false)
        }

    LaunchedEffect(Unit) {
        generalPoolViewModel.events.collect { event ->
            when (event) {
                is GeneralPoolUiEvent.RequestLocationPermission -> {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        generalPoolViewModel.ensureLocationReady(
            context,
            promptIfMissing = true
        )
    }

    // we will use this focusOnOrder when we search orders and click on it to be showing on the map
    val focusOnOrder: (OrderInfo, Boolean) -> Unit = { order, closeSearch ->
        // which order is selected
        generalPoolViewModel.onOrderSelected(order)

        // move the marker on the map to that order's location
        markerState.position = LatLng(order.lat, order.lng)

        // animate the map camera to zoom in on the order
        scope.launch {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(order.lat, order.lng), ORDER_FOCUS_ZOOM),
            )
        }

        // close the search UI
        if (closeSearch) {
            generalPoolViewModel.onSearchingChange(false)
            generalPoolViewModel.onSearchTextChange("")
        }
    }

    val headerUiModel = remember {
        HeaderUiModel(
            title = "General Pool", showStartIcon = true,
            onStartClick = { navController.popBackStack() }, showEndIcon = true,
        )
    }

    val searchController = SearchController(
        searching = ui.searching, searchText = ui.searchText,
        onSearchingChange = generalPoolViewModel::onSearchingChange,
        onSearchTextChange = generalPoolViewModel::onSearchTextChange,
    )

    Scaffold(
        // custom header with search bar
        topBar = {
            customHeader(
                modifier = Modifier.statusBarsPadding(),
                uiModel = headerUiModel, search = searchController,
            )
        },
        // bottom list of orders appears only when we have orders
        bottomBar = {
            if (ui.isLoading) {
                Text(
                    text = "Loading orders…", modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else if (ui.mapOrders.isNotEmpty()) {
                customBottom(
                    orders = ui.mapOrders,
                    onOrderClick = { order ->
                        // select + focus camera on tapped order
                        generalPoolViewModel.onOrderSelected(order)
                        markerState.position = LatLng(order.lat, order.lng)
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(order.lat, order.lng), ORDER_FOCUS_ZOOM,
                                ),
                            )
                        }
                    },
                    onCenteredOrderChange = { order, _ ->
                        // keep selection/camera in sync with the centered card
                        generalPoolViewModel.onOrderSelected(order)
                        markerState.position = LatLng(order.lat, order.lng)
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(order.lat, order.lng), ORDER_FOCUS_ZOOM,
                                ),
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            // Search dropdown: shows filtered orders; picking one focuses the map and closes search
            searchResultsDropdown(
                visible = ui.searching && ui.searchText.isNotBlank()
                        && ui.filteredOrders.isNotEmpty(),
                orders = ui.filteredOrders, onPick = { focusOnOrder(it, true) },
            )

            // Distance filter slider: enabled only after location permission is granted
            distanceFilterBar(
                maxDistanceKm = ui.distanceThresholdKm,
                onMaxDistanceKm = generalPoolViewModel::onDistanceChange,
                enabled = ui.hasLocationPerm,
            )

            // Google Map with circles and a single marker for the selected order
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
        // draw a circle for each order
        orders.forEach { order ->
            val isSelected = selected?.orderNumber == order.orderNumber

            // Scale radius inversely with zoom
            val baseRadius =
                if (isSelected) {
                    integerResource(id = R.integer.selected_circle_radius_m)
                } else {
                    integerResource(
                        id = R.integer.unselected_circle_radius_m,
                    )
                }

            val scaledRadius =
                baseRadius * (integerResource(id = R.integer.zoom_normalizer) / zoom.toDouble())

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

        // keep the marker pinned to the currently selected order
        LaunchedEffect(selected?.lat, selected?.lng) {
            selected?.let { sel ->
                markerState.position = LatLng(sel.lat, sel.lng)
            }
        }

        // render the single marker only if we have a selection
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
    val distanceRangeKm =
        (
                integerResource(id = R.integer.min_distance_km)..integerResource(id = R.integer.max_distance_km) step
                        integerResource(
                            id = R.integer.step_distance_km,
                        )
                ).map { it.toFloat() }

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
            // when permission is not yet granted
            Text(
                text = "Please allow location access to enable distance filtering.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun searchResultsDropdown(
    visible: Boolean,
    orders: List<OrderInfo>,
    onPick: (OrderInfo) -> Unit,
) {
    AnimatedVisibility(
        visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            tonalElevation = 2.dp,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                orders.forEach { order ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(order) }
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
}
