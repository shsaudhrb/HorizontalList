package com.ntg.lmd.di

import com.ntg.lmd.mainscreen.data.datasource.remote.GetUsersApi
import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.mainscreen.data.datasource.remote.UpdatetOrdersStatusApi
import com.ntg.lmd.mainscreen.data.repository.MyOrdersRepositoryImpl
import com.ntg.lmd.mainscreen.data.repository.UpdateOrdersStatusRepositoryImpl
import com.ntg.lmd.mainscreen.data.repository.UsersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.repository.MyOrdersRepository
import com.ntg.lmd.mainscreen.domain.repository.UpdateOrdersStatusRepository
import com.ntg.lmd.mainscreen.domain.repository.UsersRepository
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetActiveUsersUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.domain.usecase.UpdateOrderStatusUseCase
import com.ntg.lmd.mainscreen.ui.viewmodel.ActiveAgentsViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.MyPoolViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.utils.SecureUserStore
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val networkModule =
    module {
        single<OrdersApi> { RetrofitProvider.ordersApi }
        single<UpdatetOrdersStatusApi> { RetrofitProvider.updateStatusApi }
        single<GetUsersApi> { RetrofitProvider.usersApi }
    }

val secureUserStoreModule =
    module {
        single { SecureUserStore(get()) }
    }

val MyOrderMyPoolModule =
    module {
        // Repos
        single<MyOrdersRepository> { MyOrdersRepositoryImpl(get()) }
        single<UpdateOrdersStatusRepository> { UpdateOrdersStatusRepositoryImpl(get()) }
        single<UsersRepository> { UsersRepositoryImpl(get()) }

        // UseCases
        factory { GetMyOrdersUseCase(get()) }
        factory { UpdateOrderStatusUseCase(get()) }
        factory { GetActiveUsersUseCase(get()) }
        factory { ComputeDistancesUseCase() }

        // ViewModels
        viewModel {
            MyOrdersViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            MyPoolViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            UpdateOrderStatusViewModel(
                get(),
                get(),
            )
        }
        viewModel { ActiveAgentsViewModel(get()) }
    }
