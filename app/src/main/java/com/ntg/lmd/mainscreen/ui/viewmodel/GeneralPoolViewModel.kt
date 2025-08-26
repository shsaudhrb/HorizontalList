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
import com.google.android.gms.maps.model.LatLng
import com.ntg.lmd.mainscreen.domain.model.GeneralPoolUiState
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.mapper.toUi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    // hold the device lat/lng (current location)
    private val _deviceLatLng = MutableStateFlow<LatLng?>(null)
    val deviceLatLng: StateFlow<LatLng?> = _deviceLatLng.asStateFlow()

    // Context reference
    @SuppressLint("StaticFieldLeak")
    private lateinit var ctx: Context

    // distance computation use case
    private val computeDistances by lazy { GeneralPoolProvider.computeDistancesUseCase() }

    // Cache last non-empty orders to guard against empty realtime emissions
    private var lastNonEmptyOrders: List<OrderInfo> = emptyList()

    fun attach(context: Context) {
        if (::ctx.isInitialized) return
        ctx = context.applicationContext
        loadOrdersFromApi()
        ensureLocationReady(ctx, promptIfMissing = true)
    }

    // toggle the search mode, for showing/hiding the search fields and results
    fun onSearchingChange(v: Boolean) = _ui.update { it.copy(searching = v) }

    // update the search text for filtering orders
    fun onSearchTextChange(v: String) = _ui.update { it.copy(searchText = v) }

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
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val granted = fineGranted || coarseGranted
        _ui.update { it.copy(hasLocationPerm = granted) }
        if (granted) {
            // if already have permission -> calculate distance
            fetchAndApplyDistances(context)
        } else if (promptIfMissing) {
            // if not, request permission
            _events.tryEmit(GeneralPoolUiEvent.RequestLocationPermission)
        }
    }

    // update orders list with calculated distances
    private fun applyDistancesFrom(origin: Location) {
        val updated = computeDistances(origin, _ui.value.orders)

        _ui.update { it.copy(orders = updated) }
        if (updated.isNotEmpty()) lastNonEmptyOrders = updated

        _deviceLatLng.value = LatLng(origin.latitude, origin.longitude)
        ensureSelectedStillVisible()
    }

    // fetch device location, compute distance to each order, and sort by nearest first
    fun fetchAndApplyDistances(context: Context) {
        if (_ui.value.orders.isEmpty()) return
        viewModelScope.launch {
            val (last, current) = GeneralPoolProvider.getDeviceLocationsUseCase().invoke(context)
            when {
                current != null -> applyDistancesFrom(current)
                last != null -> applyDistancesFrom(last)
                else -> Log.d("GeneralPoolVM", "No device location yet")
            }
        }
    }

    // Fetch orders from API
    private fun loadOrdersFromApi() {
        _ui.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val loadUseCase = GeneralPoolProvider.loadOrdersUseCase(ctx)
            loadUseCase().collectLatest { result ->
                result.onSuccess { resp ->
                    val data = resp.data

                    // Initial orders from API response
                    val initial = data?.initialOrders.orEmpty().map { it.toUi(ctx) }

                    _ui.update { it.copy(orders = initial, isLoading = false) }
                    if (initial.isNotEmpty()) lastNonEmptyOrders = initial

                    // Apply distances if we already have permission
                    if (_ui.value.hasLocationPerm) {
                        fetchAndApplyDistances(ctx)
                    }

                    ensureSelectedStillVisible()

                    // Subscribe to realtime channel
                    val channel =
                        data?.realtimeConfig?.channelName ?: data?.channelName ?: "live-orders"
                    GeneralPoolProvider.ordersRepository(ctx).connectToOrders(channel)

                    // Collect realtime updates
                    viewModelScope.launch {
                        val repo =
                            GeneralPoolProvider.ordersRepository(ctx) as? com.ntg.lmd.mainscreen.data.repository.OrdersRepositoryImpl
                        val flow = repo?.orders() ?: return@launch

                        flow.collectLatest { list ->
                            if (list.isEmpty()) {
                                // Restore last known orders if realtime gives empty
                                val cur = _ui.value.orders.size
                                if (cur == 0 && lastNonEmptyOrders.isNotEmpty()) {
                                    _ui.update { it.copy(orders = lastNonEmptyOrders) }
                                }
                                return@collectLatest
                            }

                            // Keep previous distances when mapping new orders
                            val prevDistances =
                                _ui.value.orders.associate { it.orderNumber to it.distanceKm }
                            val mapped = list.map { raw ->
                                val uiOrder = raw.toUi(ctx)
                                uiOrder.copy(
                                    distanceKm = prevDistances[uiOrder.orderNumber]
                                        ?: Double.POSITIVE_INFINITY
                                )
                            }

                            _ui.update { cur -> cur.copy(orders = mapped) }
                            if (mapped.isNotEmpty()) lastNonEmptyOrders = mapped

                            // Resort orders with cached location if available
                            val loc = _deviceLatLng.value
                            if (loc != null) {
                                val fake = Location("cached").apply {
                                    latitude = loc.latitude
                                    longitude = loc.longitude
                                }
                                val resorted = computeDistances(fake, _ui.value.orders)
                                _ui.update { it.copy(orders = resorted) }
                                if (resorted.isNotEmpty()) lastNonEmptyOrders = resorted
                                ensureSelectedStillVisible()
                            }
                        }
                    }
                }.onFailure { e ->
                    _ui.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // ensure currently selected order is still visible in the filtered list, if not then clear selection
    fun ensureSelectedStillVisible() {
        _ui.update { s ->
            val sel = s.selected
            if (sel != null && s.mapOrders.none { it.orderNumber == sel.orderNumber }) {
                s.copy(selected = null)
            } else s
        }
    }
}
