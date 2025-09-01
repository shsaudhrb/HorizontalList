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
import com.ntg.lmd.mainscreen.data.model.Order
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository
import com.ntg.lmd.mainscreen.ui.mapper.toUi
import com.ntg.lmd.mainscreen.ui.model.GeneralPoolUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.map

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

    private var realtimeStarted = false
    private var realtimeJob: Job? = null

    // distance computation use case
    private val computeDistances by lazy { GeneralPoolProvider.computeDistancesUseCase() }

    // Cache last non-empty orders to guard against empty realtime emissions
    private var lastNonEmptyOrders: List<OrderInfo> = emptyList()

    // whether the user explicitly picked a card (don't auto-override selection if true)
    private var userPinnedSelection: Boolean = false

    fun attach(context: Context) {
        if (::ctx.isInitialized) return
        ctx = context.applicationContext

        val repo = GeneralPoolProvider.ordersRepository(ctx)
        if (!realtimeStarted) {
            realtimeStarted = true
            repo.connectToOrders("orders")
            observeRealtimeOrders(repo)
        }

        loadOrdersFromApi()
        ensureLocationReady(ctx, promptIfMissing = true)
    }

    override fun onCleared() {
        if (::ctx.isInitialized) {
            GeneralPoolProvider.ordersRepository(ctx).disconnectFromOrders()
        }
        realtimeJob?.cancel()
        realtimeStarted = false
        super.onCleared()
    }

    // collect live orders from socket and push to UI
    private fun observeRealtimeOrders(repo: OrdersRepository) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            repo.orders().collect { liveOrders ->
                // map -> UI
                val incoming = liveOrders.map { it.toUi(ctx) }

                // merge with existing by orderNumber
                val merged = mergeOrders(_ui.value.orders, incoming)

                // keep selection policy
                val currentSel = _ui.value.selected
                val nextSel =
                    when {
                        userPinnedSelection -> currentSel
                        currentSel == null -> merged.firstOrNull { it.lat != 0.0 && it.lng != 0.0 }
                            ?: merged.firstOrNull()

                        merged.none { it.orderNumber == currentSel.orderNumber } -> null
                        else -> merged.firstOrNull { it.orderNumber == currentSel.orderNumber }
                    }

                _ui.update {
                    it.copy(
                        orders = merged,
                        selected = nextSel ?: it.selected,
                    )
                }

                if (merged.isNotEmpty()) lastNonEmptyOrders = merged

                // if we already have location permission, re-apply distances so new orders get distance & filters
                if (_ui.value.hasLocationPerm) {
                    fetchAndApplyDistances(ctx)
                }

                ensureSelectedStillVisible()
            }
        }
    }

    private fun mergeOrders(
        existing: List<OrderInfo>,
        incoming: List<OrderInfo>,
    ): List<OrderInfo> {
        if (existing.isEmpty()) return incoming
        if (incoming.isEmpty()) return existing

        val map = existing.associateBy { it.orderNumber }.toMutableMap()
        for (o in incoming) map[o.orderNumber] = o
        return map.values.toList()
    }

    // toggle the search mode, for showing/hiding the search fields and results
    fun onSearchingChange(v: Boolean) = _ui.update { it.copy(searching = v) }

    // update the search text for filtering orders
    fun onSearchTextChange(v: String) = _ui.update { it.copy(searchText = v) }

    // change the max distance, used to filter map orders
    fun onDistanceChange(km: Double) {
        _ui.update { it.copy(distanceThresholdKm = km) }
        ui.ensureSelectedStillVisible { _ui.update(it) }
    }

    fun onOrderSelected(order: OrderInfo?) {
        userPinnedSelection = order != null
        _ui.update { it.copy(selected = order) }
    }

    fun ensureLocationReady(
        context: Context,
        promptIfMissing: Boolean,
    ) {
        val fineGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        val granted = fineGranted || coarseGranted
        _ui.update { it.copy(hasLocationPerm = granted) }
        if (granted) {
            fetchAndApplyDistances(context)
        } else if (promptIfMissing) {
            _events.tryEmit(GeneralPoolUiEvent.RequestLocationPermission)
        }
    }

    fun fetchAndApplyDistances(context: Context) {
        if (_ui.value.orders.isEmpty()) return
        viewModelScope.launch {
            val (last, current) = GeneralPoolProvider.getDeviceLocationsUseCase().invoke(context)
            val origin: Location? = current ?: last

            if (origin != null) {
                val updated = computeDistances(origin, _ui.value.orders)
                val nearest: OrderInfo? = updated.minByOrNull { it.distanceKm }

                _ui.update { prev ->
                    val currentSel = prev.selected
                    val selectionHadNoDistance = currentSel?.distanceKm?.isFinite() != true

                    val nextSelected =
                        when {
                            userPinnedSelection -> currentSel
                            currentSel == null -> nearest
                            selectionHadNoDistance -> nearest
                            else -> currentSel
                        }

                    prev.copy(orders = updated, selected = nextSelected)
                }

                if (updated.isNotEmpty()) lastNonEmptyOrders = updated
                _deviceLatLng.value = LatLng(origin.latitude, origin.longitude)
                ui.ensureSelectedStillVisible { _ui.update(it) }
            } else {
                Log.d("GeneralPoolVM", "No device location yet")
            }
        }
    }

    private fun loadOrdersFromApi() {
        _ui.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val loadUseCase = GeneralPoolProvider.loadOrdersUseCase(ctx)
            val result = loadUseCase(pageSize = 25)

            result
                .onSuccess { handleOrdersLoaded(it) }
                .onFailure { e ->
                    Log.e("GeneralPoolVM", "Failed to load paged orders: ${e.message}", e)
                    _ui.update { s -> s.copy(isLoading = false, errorMessage = "Unable to load orders.") }
                }
        }
    }

    // ----------------- Helpers (short, focused) -----------------
    private fun handleOrdersLoaded(allOrders: List<Order>) {
        val initial = allOrders.map { it.toUi(ctx) }
        val defaultSelection = pickDefaultSelection(_ui.value.selected, initial)

        userPinnedSelection = false
        _ui.update {
            it.copy(
                orders = initial,
                isLoading = false,
                selected = defaultSelection,
                errorMessage = null,
            )
        }

        if (initial.isNotEmpty()) lastNonEmptyOrders = initial
        if (_ui.value.hasLocationPerm) fetchAndApplyDistances(ctx)
        ui.ensureSelectedStillVisible { _ui.update(it) }
    }
}

private fun StateFlow<GeneralPoolUiState>.ensureSelectedStillVisible(
    update: (
        GeneralPoolUiState.()
        -> GeneralPoolUiState,
    ) -> Unit,
) {
    update {
        val sel = selected
        if (sel != null && mapOrders.none { it.orderNumber == sel.orderNumber }) {
            copy(selected = null)
        } else {
            this
        }
    }
}

private fun pickDefaultSelection(
    current: OrderInfo?,
    initial: List<OrderInfo>,
): OrderInfo? =
    current
        ?: initial.firstOrNull { it.lat != 0.0 && it.lng != 0.0 }
        ?: initial.firstOrNull()
