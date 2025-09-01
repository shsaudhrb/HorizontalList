@file:Suppress("DEPRECATION")

package com.ntg.lmd.navigation

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ntg.lmd.MyApp
import com.ntg.lmd.R
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModel
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModelFactory
import com.ntg.lmd.mainscreen.domain.model.SearchController
import com.ntg.lmd.navigation.component.AppBarConfig
import com.ntg.lmd.navigation.component.appScaffoldWithDrawer
import com.ntg.lmd.navigation.component.drawerNavGraph
import com.ntg.lmd.navigation.component.navigateSingleTop
import com.ntg.lmd.notification.ui.viewmodel.DeepLinkViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import com.ntg.lmd.authentication.ui.screens.login.loginScreen as LoginScreen
import com.ntg.lmd.authentication.ui.screens.register.registerScreen as RegisterScreen
import com.ntg.lmd.authentication.ui.screens.splash.splashScreen as SplashScreen

@Composable
fun appNavGraph(
    rootNavController: NavHostController,
    deeplinkVM: DeepLinkViewModel,
) {
    NavHost(
        navController = rootNavController,
        startDestination = Screen.Splash.route,
    ) {
        // ---------- Splash ----------
        composable(Screen.Splash.route) {
            SplashScreen(
                navController = rootNavController,
                onDecide = { isLoggedIn ->
                    val wantsNotifications = deeplinkVM.consumeOpenNotifications()
                    rootNavController.navigate(
                        when {
                            !isLoggedIn -> Screen.Login.route
                            wantsNotifications -> "${Screen.Drawer.route}?openNotifications=true"
                            else -> Screen.Drawer.route
                        },
                    ) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        // ---------- Auth ----------
        composable(Screen.Login.route) { LoginScreen(navController = rootNavController) }
        composable(Screen.Register.route) { RegisterScreen(navController = rootNavController) }

        // ---------- Drawer Host (AFTER LOGIN) ----------
        composable(
            route = Screen.Drawer.route + "?openNotifications={openNotifications}",
            arguments =
                listOf(
                    navArgument("openNotifications") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            deepLinks = listOf(navDeepLink { uriPattern = "myapp://notifications" }),
        ) { backStackEntry ->
            val argOpen = backStackEntry.arguments?.getBoolean("openNotifications") ?: false

            val deepLinkIntent: Intent? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    backStackEntry.arguments?.getParcelable(
                        "android-support-nav:controller:deepLinkIntent",
                        Intent::class.java,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    backStackEntry.arguments?.getParcelable("android-support-nav:controller:deepLinkIntent")
                }
            val uri = deepLinkIntent?.data
            val deepOpen = (uri?.scheme == "myapp" && uri.host == "notifications")
            val ctx = LocalContext.current
            val settingsVm: com.ntg.lmd.settings.ui.viewmodel.SettingsViewModel =
                viewModel(
                    factory =
                        com.ntg.lmd.settings.ui.viewmodel.SettingsViewModelFactory(
                            ctx.applicationContext as Application,
                        ),
                )

            val logoutState by settingsVm.logoutState.collectAsState()

            LaunchedEffect(logoutState) {
                when (logoutState) {
                    is com.ntg.lmd.settings.data.LogoutUiState.Success -> {
                        rootNavController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Drawer.route) { inclusive = true }
                            launchSingleTop = true
                        }
                        settingsVm.resetLogoutState()
                    }
                    is com.ntg.lmd.settings.data.LogoutUiState.Error -> {
                        settingsVm.resetLogoutState()
                    }
                    else -> Unit
                }
            }
            drawerHost(
                onLogout = { settingsVm.logout() },
                openNotifications = argOpen || deepOpen,
            )
        }
    }
}

// ======================= Drawer host =======================

@Composable
private fun drawerHost(
    onLogout: () -> Unit,
    openNotifications: Boolean = false,
) {
    val drawerNavController = rememberNavController()
    val backStack by drawerNavController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.GeneralPool.route
    val startDest = if (openNotifications) Screen.Notifications.route else Screen.GeneralPool.route

    val ctx = LocalContext.current
    val app = ctx.applicationContext as MyApp

    val loginVm: LoginViewModel =
        viewModel(
            factory = LoginViewModelFactory(ctx.applicationContext as Application),
        )
    val loginUi by loginVm.uiState.collectAsState()
    val effectiveUserName = loginUi.displayName ?: app.authRepo.lastLoginName

    var openOrdersHistoryMenu by remember { mutableStateOf<(() -> Unit)?>(null) }
    val spec = buildRouteUiSpec(currentRoute, drawerNavController, openOrdersHistoryMenu)
    val search = rememberSearchController(drawerNavController)
    val topBar = buildTopBar(spec, search)

    val inSettingsSub by remember(backStack) {
        backStack?.savedStateHandle?.getStateFlow("settings_in_sub", false)
            ?: MutableStateFlow(false)
    }.collectAsState()

    val hideChrome = currentRoute.startsWith(Screen.Settings.route) && inSettingsSub
    val showChrome = !hideChrome

    appScaffoldWithDrawer(
        navConfig =
            AppNavConfig(
                navController = drawerNavController,
                currentRoute = currentRoute,
            ),
        topBar = topBar,
        appBar = AppBarConfig(title = spec.title),
        onLogout = onLogout,
        userName = effectiveUserName,
        showChrome = showChrome,
    ) {
        drawerNavGraph(
            navController = drawerNavController,
            startDestination = startDest,
            registerOpenMenu = { setter -> openOrdersHistoryMenu = setter },
            externalQuery = search.text.value,
            onOpenOrderDetails = { id -> drawerNavController.navigate("order/$id") },
        )
    }
}

// ======================= Helpers =======================

@Composable
private fun rememberSearchController(navController: NavHostController): SearchController {
    val searching = remember { mutableStateOf(false) }
    val text = remember { mutableStateOf("") }

    return remember(searching, text) {
        SearchController(
            searching = searching,
            text = text,
            onSubmit = { q ->
                navController.currentBackStackEntry?.savedStateHandle?.set("search_submit", q)
            },
            onToggle = { enabled ->
                searching.value = enabled
                navController.currentBackStackEntry?.savedStateHandle?.set("searching", enabled)
            },
            onTextChange = { t ->
                text.value = t
                navController.currentBackStackEntry?.savedStateHandle?.set("search_text", t)
            },
        )
    }
}

@Composable
private fun buildTopBar(
    spec: RouteUiSpec,
    search: SearchController,
): TopBarConfigWithTitle =
    TopBarConfigWithTitle(
        title = spec.title,
        search = search,
        showSearchIcon = spec.showSearchIcon,
        actionButtonLabel = spec.actionButtonLabel,
        onActionButtonClick = spec.onActionButtonClick,
        actionIcon = spec.actionIcon,
        onActionIconClick = spec.onActionIconClick,
        searchPlaceholder = spec.searchPlaceholder,
        searchActionIcon = if (spec.showSearchIcon) Icons.Filled.Search else null,
        onSearchIconClick = { search.onToggle(true) },
    )

// ======================= Route UI Spec =======================

@Composable
private fun buildRouteUiSpec(
    currentRoute: String,
    nav: NavHostController,
    openOrdersHistoryMenu: (() -> Unit)?,
): RouteUiSpec =
    when (currentRoute) {
        Screen.GeneralPool.route ->
            RouteUiSpec(
                title = stringResource(R.string.menu_general_pool),
                showSearchIcon = true,
                searchPlaceholder = stringResource(R.string.search_order_number_customer_name),
                actionButtonLabel = stringResource(R.string.my_pool),
                onActionButtonClick = { nav.navigateSingleTop(Screen.MyPool.route) },
            )

        Screen.MyPool.route ->
            RouteUiSpec(
                title = stringResource(R.string.my_pool),
                showSearchIcon = false,
                actionButtonLabel = stringResource(R.string.menu_general_pool),
                onActionButtonClick = { nav.navigateSingleTop(Screen.GeneralPool.route) },
            )

        Screen.MyOrders.route ->
            RouteUiSpec(
                title = stringResource(R.string.menu_my_orders),
                showSearchIcon = true,
                searchPlaceholder = stringResource(R.string.search_order_number),
            )

        Screen.DeliveriesLog.route ->
            RouteUiSpec(
                title = stringResource(R.string.menu_deliveries_log),
                showSearchIcon = true,
                searchPlaceholder = stringResource(R.string.search_order_number),
            )

        Screen.OrdersHistory.route ->
            RouteUiSpec(
                title = stringResource(R.string.menu_order_history),
                showSearchIcon = false,
                actionIcon = Icons.Filled.MoreVert,
                onActionIconClick = { openOrdersHistoryMenu?.invoke() },
            )

        Screen.Notifications.route ->
            RouteUiSpec(
                title = stringResource(R.string.menu_notifications),
                showSearchIcon = false,
            )

        Screen.Settings.route ->
            RouteUiSpec(
                title = stringResource(R.string.menu_settings),
                showSearchIcon = false,
            )

        Screen.Chat.route ->
            RouteUiSpec(
                title = stringResource(R.string.menu_chat),
                showSearchIcon = false,
            )

        else -> RouteUiSpec(title = stringResource(R.string.app_name), showSearchIcon = false)
    }
