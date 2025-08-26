package com.ntg.lmd.mainscreen.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
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
import java.io.IOException

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

    // location client held as a property and cleared in onCleared()
    private var fusedLocationClient: FusedLocationProviderClient? = null

    // meters -> kilometers
    companion object {
        private const val METERS_IN_KILOMETER = 1000.0
    }

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
    fun ensureLocationReady(
        context: Context,
        promptIfMissing: Boolean,
    ) {
        val fine =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        val coarse =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        val granted = fine || coarse

        _ui.update { it.copy(hasLocationPerm = granted) }

        if (granted) {
            // permission already granted -> fetch device location and distances
            if (fusedLocationClient == null) {
                val appCtx = context.applicationContext
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(appCtx)
            }
            fetchAndApplyDistances()
        } else if (promptIfMissing) {
            // permission missing -> ask UI layer to request it
            _events.tryEmit(GeneralPoolUiEvent.RequestLocationPermission)
        }
    }

    // for loading orders from assets (orders.json)
    fun loadOrdersFromAssets(context: Context) {
        viewModelScope.launch {
            val list =
                try {
                    val json =
                        context.assets
                            .open("orders.json")
                            .bufferedReader()
                            .use { it.readText() }
                    val type = object : TypeToken<List<OrderInfo>>() {}.type
                    Gson().fromJson<List<OrderInfo>>(json, type) ?: emptyList()
                } catch (e: IOException) {
                    Log.e("Orders", "Failed to read orders.json from assets", e)
                    emptyList()
                } catch (e: JsonSyntaxException) {
                    Log.e("Orders", "Invalid JSON format in orders.json", e)
                    emptyList()
                }

            _ui.update {
                it.copy(
                    orders = list.map { o -> o.copy(distanceKm = Double.POSITIVE_INFINITY) },
                    isLoading = false,
                )
            }
            ensureSelectedStillVisible()
        }
    }

    // for calculating distance between two coordinates (in km)
    private fun calculateDistanceKm(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0] / METERS_IN_KILOMETER
    }

    // fetch device location, compute distance to each order, and sort by nearest first
    @SuppressLint("MissingPermission")
    fun fetchAndApplyDistances() {
        val client = fusedLocationClient ?: return
        val currentOrders = _ui.value.orders
        if (currentOrders.isEmpty()) return

        fun List<OrderInfo>.withDistancesFrom(loc: Location): List<OrderInfo> =
            map { o ->
                o.copy(distanceKm = calculateDistanceKm(loc.latitude, loc.longitude, o.lat, o.lng))
            }.sortedBy { it.distanceKm }

        fun applyFrom(loc: Location) {
            _deviceLatLng.value = LatLng(loc.latitude, loc.longitude)
            _ui.update { it.copy(orders = currentOrders.withDistancesFrom(loc)) }
            ensureSelectedStillVisible()
        }

        // lastLocation (fast, may be null/outdated)
        client.lastLocation
            .addOnSuccessListener { last ->
                if (last != null) applyFrom(last)
            }

        // getCurrentLocation
        client
            .getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null,
            ).addOnSuccessListener { loc ->
                if (loc != null) applyFrom(loc)
            }.addOnFailureListener {
            }
    }

    fun ensureSelectedStillVisible() =
        _ui.update { s ->
            val sel = s.selected
            if (sel != null && s.mapOrders.none { it.orderNumber == sel.orderNumber }) {
                s.copy(selected = null)
            } else {
                s
            }
        }

    // clean up
    override fun onCleared() {
        super.onCleared()
        fusedLocationClient = null
    }
}
