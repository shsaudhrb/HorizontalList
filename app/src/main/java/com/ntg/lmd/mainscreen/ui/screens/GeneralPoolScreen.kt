package com.ntg.lmd.mainscreen.ui.screens

import android.app.Application
import android.content.Context
import android.widget.Toast
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
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.components.distanceFilterBar
import com.ntg.lmd.mainscreen.ui.components.locationPermissionHandler
import com.ntg.lmd.mainscreen.ui.components.mapCenter
import com.ntg.lmd.mainscreen.ui.components.poolBottomContent
import com.ntg.lmd.mainscreen.ui.components.searchResultsDropdown
import com.ntg.lmd.mainscreen.ui.model.GeneralPoolUiState
import com.ntg.lmd.mainscreen.ui.model.MapStates
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModelFactory
import com.ntg.lmd.network.core.RetrofitProvider.userStore
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

// Map / Camera behavior
private const val INITIAL_MAP_ZOOM = 12f
private const val ORDER_FOCUS_ZOOM = 14f

@Composable
fun generalPoolScreen(
    navController: NavController,
    generalPoolViewModel: GeneralPoolViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val ui by generalPoolViewModel.ui.collectAsStateWithLifecycle()
    val cameraPositionState = rememberCameraPositionState()
    val markerState = remember { MarkerState(LatLng(0.0, 0.0)) }
    val scope = rememberCoroutineScope()
    val deviceLatLng by generalPoolViewModel.deviceLatLng.collectAsStateWithLifecycle()
    val hasCenteredOnDevice = remember { mutableStateOf(false) }

    setupInitialCamera(ui, deviceLatLng, cameraPositionState, hasCenteredOnDevice)
    val currentUserId = remember { userStore.getUserId() }

    LaunchedEffect(Unit) {
        generalPoolViewModel.setCurrentUserId(currentUserId)
        generalPoolViewModel.attach()
    }

    locationPermissionHandler(
        onPermissionGranted = {
            generalPoolViewModel.handleLocationPermission(granted = true)
        },
        onPermissionDenied = {
            generalPoolViewModel.handleLocationPermission(granted = false, promptIfMissing = true)
        },
    )

    rememberSearchEffects(navController, generalPoolViewModel)

    val focusOnOrder =
        rememberFocusOnOrder(generalPoolViewModel, markerState, cameraPositionState, scope)
    val onAddToMe = addToMeAction(context, generalPoolViewModel, currentUserId)

    Box(Modifier.fillMaxSize()) {
        generalPoolContent(
            ui = ui,
            focusOnOrder = focusOnOrder,
            onMaxDistanceKm = generalPoolViewModel::onDistanceChange,
            mapStates = MapStates(cameraPositionState, markerState),
            deviceLatLng = deviceLatLng,
        )
        poolBottomContent(
            ui = ui,
            viewModel = generalPoolViewModel,
            focusOnOrder = focusOnOrder,
            onAddToMe = onAddToMe,
        )
    }
}

@Composable
private fun addToMeAction(
    context: Context,
    viewModel: GeneralPoolViewModel,
    currentUserId: String?,
): (OrderInfo) -> Unit {
    val app = LocalContext.current.applicationContext as Application
    val updateVm: UpdateOrderStatusViewModel =
        viewModel(factory = UpdateOrderStatusViewModelFactory(app))
    val scope = rememberCoroutineScope()

    return remember(currentUserId) {
        { order ->
            val uid = currentUserId
            if (uid.isNullOrBlank()) return@remember
            val status = order.status ?: OrderStatus.ADDED
            viewModel.onOrderSelected(order.copy(assignedAgentId = uid))
            scope.launch {
                runCatching {
                    updateVm.update(
                        orderId = order.id,
                        targetStatus = status,
                        assignedAgentId = uid,
                    )
                }.onSuccess {
                    viewModel.onOrderSelected(null)
                    viewModel.removeOrderFromPool(order.id)
                    Toast.makeText(context, "Order Added Successfully", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Failed to add order", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
