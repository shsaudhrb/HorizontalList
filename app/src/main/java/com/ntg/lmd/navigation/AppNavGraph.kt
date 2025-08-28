@file:Suppress("DEPRECATION")

package com.ntg.lmd.navigation

import android.content.Intent
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.SearchController
import com.ntg.lmd.mainscreen.ui.screens.chatScreen
import com.ntg.lmd.mainscreen.ui.screens.deliveriesLogScreen
import com.ntg.lmd.mainscreen.ui.screens.generalPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.myOrdersScreen
import com.ntg.lmd.mainscreen.ui.screens.myPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.orderDetailsScreen
import com.ntg.lmd.navigation.component.AppBarConfig
import com.ntg.lmd.navigation.component.appScaffoldWithDrawer
import com.ntg.lmd.navigation.component.navigateSingleTop
import com.ntg.lmd.notification.ui.screens.notificationScreen
import com.ntg.lmd.notification.ui.viewmodel.DeepLinkViewModel
import com.ntg.lmd.order.ui.screen.ordersHistoryRoute
import com.ntg.lmd.settings.ui.screens.settingsOptions
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

            drawerHost(
                onLogout = {
                    rootNavController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Drawer.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
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

    // Jump to notifications if requested
    LaunchedEffect(openNotifications) {
        if (openNotifications) {
            drawerNavController.navigate(Screen.Notifications.route) { launchSingleTop = true }
        }
    }

    // Shared hook for Orders History overflow menu
    var openOrdersHistoryMenu by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Route-specific UI
    val spec = buildRouteUiSpec(currentRoute, drawerNavController, openOrdersHistoryMenu)

    // Shared search controller
    val search = rememberSearchController(drawerNavController)

    // Top bar config
    val topBar = buildTopBar(spec, search)

    // Scaffold + inner nav
    appScaffoldWithDrawer(
        navConfig =
            AppNavConfig(
                navController = drawerNavController,
                currentRoute = currentRoute,
            ),
        topBar = topBar,
        onLogout = onLogout,
        appBar =
            AppBarConfig(
                // required by your scaffold
                title = spec.title,
            ),
    ) {
        drawerNavGraph(
            navController = drawerNavController,
            startDestination = startDest,
            registerOpenMenu = { setter -> openOrdersHistoryMenu = setter },
            externalQuery = search.text.value, // pass String
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

@Composable
private fun drawerNavGraph(
    navController: NavHostController,
    startDestination: String,
    registerOpenMenu: (setter: (() -> Unit)?) -> Unit,
    externalQuery: String,
    onOpenOrderDetails: (String) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.GeneralPool.route) { generalPoolScreen(navController) }

        composable(Screen.MyOrders.route) {
            myOrdersScreen(
                navController = navController,
                externalQuery = externalQuery,
                onOpenOrderDetails = onOpenOrderDetails,
            )
        }

        composable(
            route = "order/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val idStr = backStackEntry.arguments?.getString("id")
            val id = idStr
            orderDetailsScreen(
                orderId = id, // nullable is OK
                navController = navController,
            )
        }

        composable(
            route = Screen.Notifications.route,
            deepLinks = listOf(navDeepLink { uriPattern = "myapp://notifications" }),
        ) { notificationScreen() }

        composable(Screen.OrdersHistory.route) {
            ordersHistoryRoute(registerOpenMenu = registerOpenMenu)
        }

        composable(Screen.DeliveriesLog.route) { deliveriesLogScreen(navController) }
        composable(Screen.Settings.route) { settingsOptions(navController) }
        composable(Screen.MyPool.route) {
            myPoolScreen(
                onOpenOrderDetails = onOpenOrderDetails,
            )
        }
        composable(Screen.Chat.route) { chatScreen() }
    }
}

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
