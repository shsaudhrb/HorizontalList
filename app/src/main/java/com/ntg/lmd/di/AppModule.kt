package com.ntg.lmd.di

import com.ntg.lmd.BuildConfig
import com.ntg.lmd.authentication.data.datasource.remote.api.AuthApi
import com.ntg.lmd.authentication.data.repositoryImp.AuthRepositoryImp
import com.ntg.lmd.authentication.domain.repository.AuthRepository
import com.ntg.lmd.authentication.domain.usecase.LoginUseCase
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModel
import com.ntg.lmd.mainscreen.data.repository.DeliveriesLogRepositoryImpl
import com.ntg.lmd.mainscreen.domain.repository.DeliveriesLogRepository
import com.ntg.lmd.mainscreen.domain.usecase.GetDeliveriesLogFromApiUseCase
import com.ntg.lmd.mainscreen.ui.viewmodel.DeliveriesLogViewModel
import com.ntg.lmd.network.authheader.AuthInterceptor
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.connectivity.NetworkMonitor
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.network.sockets.SocketIntegration
import com.ntg.lmd.order.data.remote.OrdersHistoryApi
import com.ntg.lmd.order.data.remote.repository.OrdersRepositoryImpl
import com.ntg.lmd.order.domain.model.repository.OrdersRepository
import com.ntg.lmd.order.domain.model.usecase.GetOrdersUseCase
import com.ntg.lmd.order.ui.viewmodel.OrderHistoryViewModel
import com.ntg.lmd.settings.data.SettingsPreferenceDataSource
import com.ntg.lmd.settings.ui.viewmodel.SettingsViewModel
import com.ntg.lmd.utils.LogoutManager
import com.ntg.lmd.utils.SecureUserStore
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory import com.ntg.lmd.mainscreen.data.datasource.remote.GetUsersApi
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

val ordersHistoryModule =
    module {
        single<OrdersHistoryApi> { get<Retrofit>().create(OrdersHistoryApi::class.java)}
        single<OrdersRepository> { OrdersRepositoryImpl(get()) }
        factory { GetOrdersUseCase(get()) }
        viewModel { OrderHistoryViewModel(get()) }
    }
val deliveriesLogModule =
    module {
        single<OrdersApi> { get<Retrofit>().create(OrdersApi::class.java) }
        single<DeliveriesLogRepository> { DeliveriesLogRepositoryImpl(get()) }
        factory { GetDeliveriesLogFromApiUseCase(get()) }
        viewModel { DeliveriesLogViewModel(get()) }
    }

val authModule =
    module {
        // SecureUserStore
        single { SecureUserStore(get()) }

        single {
            AuthInterceptor(
                store = get(),
                supabaseKey = BuildConfig.SUPABASE_KEY,
            )
        }

        single {
            OkHttpClient
                .Builder()
                .addInterceptor(get<AuthInterceptor>())
                .build()
        }

        single {
            Retrofit
                .Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(get<OkHttpClient>())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        // AuthApi (Retrofit no auth)
        single<AuthApi> { get<Retrofit>().create(AuthApi::class.java) }

        // Repository
        single<AuthRepository> {
            AuthRepositoryImp(
                loginApi = get(),
                store = get(), // SecureTokenStore
                userStore = get(), // SecureUserStore
            )
        }

        // UseCase
        factory { LoginUseCase(get()) }

        // ViewModel
        viewModel { LoginViewModel(loginUseCase = get()) }
    }

val networkModule =
    module {

        // SecureTokenStore (only here)
        single { SecureTokenStore(get()) }

        single<OrdersApi> { get<Retrofit>().create(OrdersApi::class.java)}

        single<UpdatetOrdersStatusApi> { get<Retrofit>().create(UpdatetOrdersStatusApi::class.java) }

        single<GetUsersApi> { get<Retrofit>().create(GetUsersApi::class.java)}
    }

val socketModule =
    module {
        single {
            SocketIntegration(
                baseWsUrl = BuildConfig.WS_BASE_URL,
                client = RetrofitProvider.okHttpForWs,
                tokenStore = get<SecureTokenStore>(),
            ).apply {
                get<SecureTokenStore>().onTokensChanged = { access, _ ->
                    reconnectIfTokenChanged(access)
                }
            }
        }
    }

val monitorModule =
    module {
        single { NetworkMonitor(get()) }
    }

val settingsModule =
    module {
        single { SettingsPreferenceDataSource(get()) }

        single {
            LogoutManager(
                tokenStore = get<SecureTokenStore>(),
                socket = get<SocketIntegration>(),
            )
        }

        viewModel {
            SettingsViewModel(
                prefs = get(),
                logoutManager = get(),
            )
        }
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
