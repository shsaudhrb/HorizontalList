package com.ntg.lmd.mainscreen.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.components.distanceFilterBar
import com.ntg.lmd.mainscreen.ui.components.mapCenter
import com.ntg.lmd.mainscreen.ui.components.poolBottomContent
import com.ntg.lmd.mainscreen.ui.components.searchResultsDropdown
import com.ntg.lmd.mainscreen.ui.model.GeneralPoolUiState
import com.ntg.lmd.mainscreen.ui.model.MapStates
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolUiEvent
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolViewModel
import kotlinx.coroutines.launch

// Map / Camera behavior
private const val INITIAL_MAP_ZOOM = 12f
private const val ORDER_FOCUS_ZOOM = 14f

@Composable
fun generalPoolScreen(
    navController: NavController,
    generalPoolViewModel: GeneralPoolViewModel = viewModel(),
) {
    val context = LocalContext.current
    val ui by generalPoolViewModel.ui.collectAsStateWithLifecycle()
    val cameraPositionState = rememberCameraPositionState()
    val markerState = remember { MarkerState(position = LatLng(0.0, 0.0)) }
    val scope = rememberCoroutineScope()
    val deviceLatLng by generalPoolViewModel.deviceLatLng.collectAsStateWithLifecycle()
    val hasCenteredOnDevice = remember { mutableStateOf(false) }

    setupInitialCamera(ui, deviceLatLng, cameraPositionState, hasCenteredOnDevice)
    LaunchedEffect(Unit) { generalPoolViewModel.attach(context) }
    locationPermissionGate(generalPoolViewModel)
    rememberSearchEffects(navController, generalPoolViewModel)

    val focusOnOrder =
        rememberFocusOnOrder(
            viewModel = generalPoolViewModel,
            markerState = markerState,
            cameraPositionState = cameraPositionState,
            scope = scope,
        )

    Box(Modifier.fillMaxSize()) {
        generalPoolContent(
            ui = ui,
            focusOnOrder = focusOnOrder,
            onMaxDistanceKm = generalPoolViewModel::onDistanceChange,
            mapStates = MapStates(cameraPositionState, markerState),
            deviceLatLng = deviceLatLng,
        )
        poolBottomContent(ui, generalPoolViewModel, focusOnOrder)
    }
}

@Composable
private fun setupInitialCamera(
    ui: GeneralPoolUiState,
    deviceLatLng: LatLng?,
    cameraPositionState: CameraPositionState,
    hasCenteredOnDevice: MutableState<Boolean>,
) {
    LaunchedEffect(deviceLatLng, ui.selected) {
        if (deviceLatLng != null && ui.selected == null && !hasCenteredOnDevice.value) {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(deviceLatLng, INITIAL_MAP_ZOOM),
            )
            hasCenteredOnDevice.value = true
        }
    }
}

@Composable
private fun generalPoolContent(
    ui: GeneralPoolUiState,
    focusOnOrder: (OrderInfo, Boolean) -> Unit,
    onMaxDistanceKm: (Double) -> Unit,
    mapStates: MapStates,
    deviceLatLng: LatLng?,
) {
    Box(Modifier.fillMaxSize()) {
        mapCenter(
            ui = ui,
            mapStates = mapStates,
            deviceLatLng = deviceLatLng,
            modifier = Modifier.fillMaxSize(),
        )
        distanceFilterOrHint(ui, onMaxDistanceKm)
        if (ui.searching && ui.searchText.isNotBlank()) {
            searchDropdown(ui, focusOnOrder)
        }
    }
}

@Composable
private fun distanceFilterOrHint(
    ui: GeneralPoolUiState,
    onMaxDistanceKm: (Double) -> Unit,
) {
    if (ui.hasLocationPerm) {
        distanceFilterRow(ui, onMaxDistanceKm)
    } else {
        locationAccessHint()
    }
}

@Composable
private fun distanceFilterRow(
    ui: GeneralPoolUiState,
    onMaxDistanceKm: (Double) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .zIndex(1f),
        horizontalArrangement = Arrangement.Center,
    ) {
        distanceFilterBar(
            maxDistanceKm = ui.distanceThresholdKm,
            onMaxDistanceKm = onMaxDistanceKm,
            enabled = true,
        )
    }
}

@Composable
private fun locationAccessHint() {
    Box(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.location_access_request),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .zIndex(1f),
        )
    }
}

@Composable
private fun searchDropdown(
    ui: GeneralPoolUiState,
    focusOnOrder: (OrderInfo, Boolean) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .zIndex(2f),
        ) {
            searchResultsDropdown(
                visible = true,
                orders = ui.filteredOrdersInRange,
                onPick = { focusOnOrder(it, true) },
            )
        }
    }
}

@Composable
private fun locationPermissionGate(viewModel: GeneralPoolViewModel) {
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.ensureLocationReady(context, promptIfMissing = false)
        }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is GeneralPoolUiEvent.RequestLocationPermission) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.ensureLocationReady(context, promptIfMissing = true)
    }
}

@Composable
private fun rememberSearchEffects(
    navController: NavController,
    viewModel: GeneralPoolViewModel,
) {
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        handle.getStateFlow("searching", false).collect { searching ->
            viewModel.onSearchingChange(searching)
            if (!searching) viewModel.onSearchTextChange("")
        }
    }

    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        handle.getStateFlow("search_text", "").collect { text ->
            viewModel.onSearchTextChange(text)
        }
    }

    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        handle.getStateFlow("search_submit", "").collect { /* ignore */ }
    }
}

@Composable
fun rememberFocusOnOrder(
    viewModel: GeneralPoolViewModel,
    markerState: MarkerState,
    cameraPositionState: CameraPositionState,
    scope: kotlinx.coroutines.CoroutineScope,
    focusZoom: Float = ORDER_FOCUS_ZOOM,
): (OrderInfo, Boolean) -> Unit {
    val vm = rememberUpdatedState(viewModel)
    val marker = rememberUpdatedState(markerState)
    val camera = rememberUpdatedState(cameraPositionState)
    val coroutineScope = rememberUpdatedState(scope)

    return remember {
        { order: OrderInfo, closeSearch: Boolean ->
            vm.value.onOrderSelected(order)
            marker.value.position = LatLng(order.lat, order.lng)
            coroutineScope.value.launch {
                camera.value.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(order.lat, order.lng),
                        focusZoom,
                    ),
                )
            }
            if (closeSearch) {
                vm.value.onSearchingChange(false)
                vm.value.onSearchTextChange("")
            }
        }
    }
}
