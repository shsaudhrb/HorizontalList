package com.ntg.lmd.di

import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.mainscreen.data.repository.DeliveriesLogRepositoryImpl
import com.ntg.lmd.mainscreen.domain.repository.DeliveriesLogRepository
import com.ntg.lmd.mainscreen.domain.usecase.GetDeliveriesLogFromApiUseCase
import com.ntg.lmd.mainscreen.ui.viewmodel.DeliveriesLogViewModel
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.order.data.remote.OrdersHistoryApi
import com.ntg.lmd.order.data.remote.repository.OrdersRepositoryImpl
import com.ntg.lmd.order.domain.model.repository.OrdersRepository
import com.ntg.lmd.order.domain.model.usecase.GetOrdersUseCase
import com.ntg.lmd.order.ui.viewmodel.OrderHistoryViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val networkBridgeModule = module {
    single<OrdersHistoryApi> { RetrofitProvider.ordersHistoryApi }
    single<OrdersApi> { RetrofitProvider.ordersApi }

}
val ordersHistoryModule = module {
    single<OrdersRepository> { OrdersRepositoryImpl(get()) }
    factory { GetOrdersUseCase(get()) }
    viewModel { OrderHistoryViewModel(get()) }
}
val deliveriesLogModule = module {
    single<DeliveriesLogRepository> { DeliveriesLogRepositoryImpl(get()) }
    factory { GetDeliveriesLogFromApiUseCase(get()) }
    viewModel { DeliveriesLogViewModel(get()) }
}