package com.ntg.lmd.mainscreen.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.ntg.lmd.mainscreen.domain.model.GeneralPoolUiState
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class GeneralPoolUiEvent {
    data object RequestLocationPermission : GeneralPoolUiEvent()
}

class GeneralPoolViewModel : ViewModel() {
    // ui state
    private val _ui = MutableStateFlow(GeneralPoolUiState())
    val ui: StateFlow<GeneralPoolUiState> = _ui.asStateFlow()

    // events for permission
    private val _events = MutableSharedFlow<GeneralPoolUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<GeneralPoolUiEvent> = _events.asSharedFlow()

    // hold the device lat/lng
    private val _deviceLatLng = MutableStateFlow<LatLng?>(null)
    val deviceLatLng: StateFlow<LatLng?> = _deviceLatLng.asStateFlow()

    // Use Cases
    private val loadOrders by lazy { GeneralPoolProvider.loadOrdersUseCase() }
    private val getDeviceLocations by lazy { GeneralPoolProvider.getDeviceLocationsUseCase() }
    private val computeDistances by lazy { GeneralPoolProvider.computeDistancesUseCase() }

    // toggle the search mode, for showing/hiding the search fields and results
    fun onSearchingChange(value: Boolean) = _ui.update { it.copy(searching = value) }

    // update the search text for filtering orders
    fun onSearchTextChange(value: String) = _ui.update { it.copy(searchText = value) }

    // change the max distance, used to filter map orders
    fun onDistanceChange(km: Double) {
        _ui.update { it.copy(distanceThresholdKm = km) }
        ensureSelectedStillVisible()
    }

    // set or clear the currently selected order
    fun onOrderSelected(order: OrderInfo?) = _ui.update { it.copy(selected = order) }

    // for location permission
    fun ensureLocationReady(context: Context, promptIfMissing: Boolean) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val granted = fineGranted || coarseGranted
        _ui.update { it.copy(hasLocationPerm = granted) }

        if (granted) {
            fetchAndApplyDistances(context)
        } else if (promptIfMissing) {
            _events.tryEmit(GeneralPoolUiEvent.RequestLocationPermission)
        }
    }

    // for loading data
    fun loadOrdersFromAssets(context: Context) {
        viewModelScope.launch {
            val list = loadOrders(context).map { it.copy(distanceKm = Double.POSITIVE_INFINITY) }
            _ui.update { it.copy(orders = list, isLoading = false) }
            ensureSelectedStillVisible()
        }
    }

    // for location and distance
    private fun applyDistancesFrom(origin: Location) {
        val updated = computeDistances(origin, _ui.value.orders)
        _ui.update { it.copy(orders = updated) }
        _deviceLatLng.value = LatLng(origin.latitude, origin.longitude)
        ensureSelectedStillVisible()
    }

    // fetch device location, compute distance to each order, and sort by nearest first
    fun fetchAndApplyDistances(context: Context) {
        if (_ui.value.orders.isEmpty()) return

        viewModelScope.launch {
            val (last, current) = getDeviceLocations(context)
            when {
                current != null -> applyDistancesFrom(current)
                last != null -> applyDistancesFrom(last)
                else -> { /* No location available; keep existing distances */
                }
            }
        }
    }

    fun ensureSelectedStillVisible() {
        _ui.update { s ->
            val sel = s.selected
            if (sel != null && s.mapOrders.none { it.orderNumber == sel.orderNumber }) {
                s.copy(selected = null)
            } else s
        }
    }
}
