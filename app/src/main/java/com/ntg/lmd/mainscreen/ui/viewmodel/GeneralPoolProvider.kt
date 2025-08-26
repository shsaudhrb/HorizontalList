package com.ntg.lmd.mainscreen.ui.viewmodel

import com.ntg.lmd.mainscreen.data.repository.LocationRepositoryImpl
import com.ntg.lmd.mainscreen.data.repository.OrdersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.repository.LocationRepository
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetDeviceLocationsUseCase
import com.ntg.lmd.mainscreen.domain.usecase.LoadOrdersUseCase

object GeneralPoolProvider {
    fun ordersRepository(): OrdersRepository = OrdersRepositoryImpl()
    fun locationRepository(): LocationRepository = LocationRepositoryImpl()

    fun loadOrdersUseCase(): LoadOrdersUseCase =
        LoadOrdersUseCase(ordersRepository())

    fun getDeviceLocationsUseCase(): GetDeviceLocationsUseCase =
        GetDeviceLocationsUseCase(locationRepository())

    fun computeDistancesUseCase(): ComputeDistancesUseCase =
        ComputeDistancesUseCase()
}