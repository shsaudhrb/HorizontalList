package com.ntg.lmd.mainscreen.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ntg.lmd.mainscreen.domain.model.GeneralPoolUiState
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GeneralPoolViewModel : ViewModel() {
    private val _ui = MutableStateFlow(GeneralPoolUiState())
    val ui: StateFlow<GeneralPoolUiState> = _ui.asStateFlow()

    fun onSearchingChange(value: Boolean) = _ui.update { it.copy(searching = value) }

    fun onQueryChange(value: String) = _ui.update { it.copy(query = value) }

    fun onDistanceChange(km: Float) {
        _ui.update { it.copy(distanceThresholdKm = km) }
        ensureSelectedStillVisible()
    }

    fun onOrderSelected(order: OrderInfo?) = _ui.update { it.copy(selected = order) }

    fun onPermissionsResult(granted: Boolean) {
        _ui.update { it.copy(hasLocationPerm = granted) }
    }

    // function to load orders from assets (orders.json)
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
                } catch (e: Exception) {
                    Log.e("Orders", "Failed to load orders.json from assets", e)
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

    // function to calculate distance between two coordinates (in km)
    private fun calculateDistanceKm(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0] / 1000.0
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @SuppressLint("MissingPermission")
    fun fetchAndApplyDistances(context: Context) {
        val fused = LocationServices.getFusedLocationProviderClient(context)

        fun List<OrderInfo>.withDistancesFrom(loc: Location): List<OrderInfo> =
            map { o ->
                o.copy(distanceKm = calculateDistanceKm(loc.latitude, loc.longitude, o.lat, o.lng))
            }.sortedBy { it.distanceKm }

        val currentOrders = _ui.value.orders
        if (currentOrders.isEmpty()) return

        fun applyFrom(loc: Location) {
            _ui.update { it.copy(orders = currentOrders.withDistancesFrom(loc)) }
            ensureSelectedStillVisible()
        }
        fused.lastLocation
            .addOnSuccessListener { last ->
                if (last != null) applyFrom(last)
            }
        fused
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
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
}
