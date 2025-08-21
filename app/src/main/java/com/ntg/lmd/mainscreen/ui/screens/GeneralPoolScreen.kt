package com.ntg.lmd.mainscreen.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.GeneralPoolUiState
import com.ntg.lmd.mainscreen.domain.model.MapStates
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.components.customBottom
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolUiEvent
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Map / Camera behavior
private const val INITIAL_MAP_ZOOM = 12f
private const val ORDER_FOCUS_ZOOM = 14f

// Slider constraints
private const val DISTANCE_MIN_KM = 1.0F
private const val DISTANCE_MAX_KM = 100.0F

// screen height
private const val TOP_OVERLAY_RATIO = 0.09f   // 9% of screen height
private const val BOTTOM_BAR_RATIO = 0.22f    // 22% of screen height

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

    // selected order and camera position to it
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(DEFAULT_CITY_CENTER, INITIAL_MAP_ZOOM)
        }
    val markerState = remember { MarkerState(position = DEFAULT_CITY_CENTER) }
    val scope = rememberCoroutineScope()

    // Load Local orders.json from assets
    LaunchedEffect(Unit) { generalPoolViewModel.loadOrdersFromAssets(context) }

    // handle location permission
    locationPermissionGate(viewModel = generalPoolViewModel)

    // we will use this focusOnOrder when we search orders and click on it to be showing on the map
    val focusOnOrder =
        rememberFocusOnOrder(
            viewModel = generalPoolViewModel,
            markerState = markerState,
            cameraPositionState = cameraPositionState,
            scope = scope,
        )

    // react to the app-bar "searching" on/off
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        handle.getStateFlow("searching", false).collect { searching ->
            generalPoolViewModel.onSearchingChange(searching)
            if (!searching) generalPoolViewModel.onSearchTextChange("")
        }
    }

    // live text from the app-bar TextField
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        handle.getStateFlow("search_text", "").collect { text ->
            generalPoolViewModel.onSearchTextChange(text)
        }
    }
    // react when user presses IME search
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        handle.getStateFlow("search_submit", "").collect { _ ->
        }
    }

    Box(Modifier.fillMaxSize()) {
        generalPoolContent(
            ui = ui,
            focusOnOrder = focusOnOrder,
            onMaxDistanceKm = generalPoolViewModel::onDistanceChange,
            mapStates = MapStates(cameraPositionState, markerState),
            innerPadding = PaddingValues(0.dp),
        )

        // Bottom list of orders
        when {
            ui.isLoading -> {
                Text(
                    text = "Loading orders…",
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            ui.mapOrders.isNotEmpty() -> {
                customBottom(
                    orders = ui.mapOrders,
                    onOrderClick = { order -> focusOnOrder(order, false) },
                    onCenteredOrderChange = { order, _ -> focusOnOrder(order, false) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun generalPoolContent(
    ui: GeneralPoolUiState,
    focusOnOrder: (OrderInfo, Boolean) -> Unit,
    onMaxDistanceKm: (Float) -> Unit,
    mapStates: MapStates,
    innerPadding: PaddingValues,
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        // Map at the bottom layer
        mapCenter(
            selected = ui.selected,
            orders = ui.mapOrders,
            cameraPositionState = mapStates.cameraPositionState,
            markerState = mapStates.markerState,
            modifier = Modifier.fillMaxSize(),
        )

        // Distance filter OVER the map
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .zIndex(1f),  // below search dropdown, above map
            horizontalArrangement = Arrangement.Center,
        ) {
            distanceFilterBar(
                maxDistanceKm = ui.distanceThresholdKm,
                onMaxDistanceKm = onMaxDistanceKm,
                enabled = ui.hasLocationPerm,
            )
        }

        // Search results should be ON TOP of both map and filter
        if (ui.searching && ui.searchText.isNotBlank() && ui.filteredOrders.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .zIndex(2f) // topmost layer
            ) {
                searchResultsDropdown(
                    visible = true,
                    orders = ui.filteredOrders,
                    onPick = { focusOnOrder(it, true) },
                )
            }
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

    // Screen-aware padding so map UI (incl. +/- and my-location button) isn't hidden
    val cfg = LocalConfiguration.current
    val screenH = cfg.screenHeightDp.dp

    val topOverlayHeight = (screenH * TOP_OVERLAY_RATIO).coerceIn(48.dp, 96.dp)
    val bottomBarHeight = if (orders.isNotEmpty()) {
        (screenH * BOTTOM_BAR_RATIO).coerceIn(128.dp, 280.dp)
    } else 0.dp

    // Enable blue dot only if we have location permission (prevents SecurityException)
    val context = LocalContext.current
    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val canShowMyLocation = hasFine || hasCoarse

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        // Blue dot + target button
        properties = MapProperties(isMyLocationEnabled = canShowMyLocation),
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
        contentPadding =
            PaddingValues(top = topOverlayHeight, bottom = bottomBarHeight),
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
    // clamp to [1, 100]
    val value = maxDistanceKm.coerceIn(DISTANCE_MIN_KM, DISTANCE_MAX_KM)

    Surface(
        modifier =
            Modifier
                .width(280.dp) // not full width
                .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Centered label like “100 Km”
            Text(
                text = "${value.roundToInt()} Km",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            // Straight track + circular thumb
            circleSlider(
                value = value,
                onValueChange = { onMaxDistanceKm(it.coerceIn(DISTANCE_MIN_KM, DISTANCE_MAX_KM)) },
                valueRange = DISTANCE_MIN_KM..DISTANCE_MAX_KM,
                enabled = enabled,
            )
        }
    }

    if (!enabled) {
        Text(
            text = "Please allow location access to enable distance filtering.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun circleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = DISTANCE_MIN_KM..DISTANCE_MAX_KM,
    enabled: Boolean = true,
) {
    val trackWidth = 220.dp
    val thumbRadius = 10.dp

    // Capture theme colors in composable scope (OK to read MaterialTheme here)
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val thumbColor: Color = MaterialTheme.colorScheme.primary

    Box(
        modifier =
            Modifier
                .width(trackWidth)
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Thin straight line
        Box(
            modifier =
                Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(trackColor),
        )

        // Draggable circular thumb
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .then(
                        if (enabled) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val posX = change.position.x.coerceIn(0f, size.width.toFloat())
                                    val fraction = posX / size.width
                                    val newValue =
                                        valueRange.start +
                                                fraction * (valueRange.endInclusive - valueRange.start)
                                    onValueChange(newValue)
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
        ) {
            val fraction =
                (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val x = size.width * fraction
            drawCircle(
                color = thumbColor,
                radius = thumbRadius.toPx(),
                center = Offset(x, size.height / 2),
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
            color = MaterialTheme.colorScheme.primaryContainer,
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

@Composable
private fun locationPermissionGate(viewModel: GeneralPoolViewModel) {
    val context = LocalContext.current

    // Launcher that re-checks readiness without prompting again (prevents loops)
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.ensureLocationReady(context, promptIfMissing = false)
        }

    // Listen for VM events that ask for a permission request
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
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

    // Initial readiness check (this one is allowed to prompt)
    LaunchedEffect(Unit) {
        viewModel.ensureLocationReady(context, promptIfMissing = true)
    }
}

// we will use this focusOnOrder when we search orders and click on it to be showing on the map
@Composable
fun rememberFocusOnOrder(
    viewModel: GeneralPoolViewModel,
    markerState: MarkerState,
    cameraPositionState: CameraPositionState,
    scope: kotlinx.coroutines.CoroutineScope,
    focusZoom: Float = ORDER_FOCUS_ZOOM,
): (OrderInfo, Boolean) -> Unit {
    // keep latest references without re-allocating the lambda on every recomposition
    val vm = rememberUpdatedState(viewModel)
    val marker = rememberUpdatedState(markerState)
    val camera = rememberUpdatedState(cameraPositionState)
    val coroutineScope = rememberUpdatedState(scope)

    return remember {
        { order: OrderInfo, closeSearch: Boolean ->
            // which order is selected
            vm.value.onOrderSelected(order)

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

            // close the search UI
            if (closeSearch) {
                vm.value.onSearchingChange(false)
                vm.value.onSearchTextChange("")
            }
        }
    }
}
