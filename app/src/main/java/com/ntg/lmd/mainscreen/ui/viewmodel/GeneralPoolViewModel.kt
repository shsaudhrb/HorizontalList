package com.ntg.lmd.mainscreen.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.FileUtils.copy
import android.provider.SyncStateContract.Helpers.update
import android.util.Log
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
    private val _ui = MutableStateFlow(GeneralPoolUiState())
    val ui: StateFlow<GeneralPoolUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<GeneralPoolUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val _deviceLatLng = MutableStateFlow<LatLng?>(null)
    val deviceLatLng: StateFlow<LatLng?> = _deviceLatLng.asStateFlow()

    @SuppressLint("StaticFieldLeak")
    private lateinit var ctx: Context

    private var realtimeStarted = false
    private var realtimeJob: Job? = null

    private val computeDistances by lazy { GeneralPoolProvider.computeDistancesUseCase() }
    private var lastNonEmptyOrders: List<OrderInfo> = emptyList()
    private var userPinnedSelection: Boolean = false

    fun attach(context: Context) {
        if (::ctx.isInitialized) return
        ctx = context.applicationContext
        val repo = GeneralPoolProvider.ordersRepository(ctx)
        if (!realtimeStarted) startRealtime(repo)
        loadOrdersFromApi()
        ensureLocationReady(ctx, promptIfMissing = true)
    }

    override fun onCleared() {
        if (::ctx.isInitialized) GeneralPoolProvider.ordersRepository(ctx).disconnectFromOrders()
        realtimeJob?.cancel()
        realtimeStarted = false
        super.onCleared()
    }

    private fun startRealtime(repo: OrdersRepository) {
        realtimeStarted = true
        repo.connectToOrders("orders")
        observeRealtimeOrders(repo)
    }

    private fun observeRealtimeOrders(repo: OrdersRepository) {
        realtimeJob?.cancel()
        realtimeJob =
            viewModelScope.launch {
                repo.orders().collect { handleLiveOrders(it) }
            }
    }

    private suspend fun handleLiveOrders(liveOrders: List<Order>) {
        val incoming = liveOrders.map { it.toUi(ctx) }
        val merged = mergeOrders(_ui.value.orders, incoming)
        val nextSel = determineNextSelection(merged, _ui.value.selected, userPinnedSelection)

        _ui.update { it.copy(orders = merged, selected = nextSel ?: it.selected) }
        if (merged.isNotEmpty()) lastNonEmptyOrders = merged
        if (_ui.value.hasLocationPerm) fetchAndApplyDistances(ctx)
        _ui.ensureSelectedStillVisible { removeInvalidSelectionIfNeeded() }
    }

    fun onSearchingChange(v: Boolean) = _ui.update { it.copy(searching = v) }

    fun onSearchTextChange(v: String) = _ui.update { it.copy(searchText = v) }

    fun onDistanceChange(km: Double) {
        _ui.update { it.copy(distanceThresholdKm = km) }
        _ui.ensureSelectedStillVisible { removeInvalidSelectionIfNeeded() }
    }

    fun onOrderSelected(order: OrderInfo?) {
        userPinnedSelection = order != null
        _ui.update { it.copy(selected = order) }
    }

    fun ensureLocationReady(
        context: Context,
        promptIfMissing: Boolean,
    ) {
        val granted = isLocationGranted(context)
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
            val origin = getCurrentDeviceLocation(context) ?: return@launch
            val updated = computeDistances(origin, _ui.value.orders)
            val nextSelected = determineSelectionAfterDistanceUpdate(_ui.value.selected, updated, userPinnedSelection)
            _ui.update(updateUiWithDistances(updated, nextSelected) { lastNonEmptyOrders = it })
            _deviceLatLng.value = LatLng(origin.latitude, origin.longitude)
            _ui.ensureSelectedStillVisible { this }
        }
    }

    private fun loadOrdersFromApi() {
        _ui.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val loadUseCase = GeneralPoolProvider.loadOrdersUseCase(ctx)
            val result = loadUseCase(pageSize = 25)
            result
                .onSuccess { handleOrdersLoaded(it) }
                .onFailure {
                    Log.e("GeneralPoolVM", "Failed to load orders: ${it.message}", it)
                    _ui.update { state -> state.copy(isLoading = false, errorMessage = "Unable to load orders.") }
                }
        }
    }

    private fun handleOrdersLoaded(allOrders: List<Order>) {
        val initial = allOrders.map { it.toUi(ctx) }
        val defaultSel = pickDefaultSelection(_ui.value.selected, initial)
        userPinnedSelection = false
        _ui.update { it.copy(orders = initial, isLoading = false, selected = defaultSel, errorMessage = null) }
        if (initial.isNotEmpty()) lastNonEmptyOrders = initial
        if (_ui.value.hasLocationPerm) fetchAndApplyDistances(ctx)
        _ui.ensureSelectedStillVisible { removeInvalidSelectionIfNeeded() }
    }
}
