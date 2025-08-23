package com.ntg.lmd.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ntg.lmd.mainscreen.domain.model.SearchController
import com.ntg.lmd.mainscreen.ui.screens.chatScreen
import com.ntg.lmd.mainscreen.ui.screens.deliveriesLogScreen
import com.ntg.lmd.mainscreen.ui.screens.generalPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.myOrdersScreen
import com.ntg.lmd.mainscreen.ui.screens.myPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.ordersHistoryScreen
import com.ntg.lmd.navigation.component.appScaffoldWithDrawer
import com.ntg.lmd.navigation.component.navigateSingleTop
import com.ntg.lmd.notification.ui.screens.notificationScreen
import com.ntg.lmd.settings.ui.screens.settingsOptions
import com.ntg.lmd.authentication.ui.screens.login.loginScreen as LoginScreen
import com.ntg.lmd.authentication.ui.screens.register.registerScreen as RegisterScreen
import com.ntg.lmd.authentication.ui.screens.splash.splashScreen as SplashScreen

@Composable
fun appNavGraph(rootNavController: NavHostController) {
    NavHost(
        navController = rootNavController,
        startDestination = Screen.Drawer.route,
    ) {
        // ---------- Splash ----------
        composable(Screen.Splash.route) {
            SplashScreen(
                navController = rootNavController,
                onDecide = { isLoggedIn ->
                    rootNavController.navigate(if (isLoggedIn) Screen.Drawer.route else Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        // ---------- Auth ----------
        composable(Screen.Login.route) { LoginScreen() }
        composable(Screen.Register.route) {
            RegisterScreen(navController = rootNavController)
        }

        // ---------- Drawer Host (after login) ----------
        composable(Screen.Drawer.route) {
            drawerHost(
                onLogout = {
                    rootNavController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Drawer.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}

@Composable
private fun drawerHost(onLogout: () -> Unit) {
    val drawerNavController = rememberNavController()
    val backStack by drawerNavController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.GeneralPool.route

    // ---- Search state (kept at nav layer so it's shared across screens) ----
    val searchingState = remember { mutableStateOf(false) } // search on/off
    val searchTextState = remember { mutableStateOf("") }   // current query

    // Build a UI "spec" for the current route
    val spec = buildRouteUiSpec(currentRoute, drawerNavController)

    // Controller that wires the app bar search field <-> VM via savedStateHandle
    val search =
        remember(searchingState, searchTextState) {
            SearchController(
                searching = searchingState,
                text = searchTextState,
                // Called when user hits IME search
                onSubmit = { query ->
                    drawerNavController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("search_submit", query)
                },
                // Called when search mode is toggled (open/close)
                onToggle = { enabled ->
                    searchingState.value = enabled
                    drawerNavController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("searching", enabled)
                },
                // Called on every keystroke in the search field
                onTextChange = { t ->
                    searchTextState.value = t
                    drawerNavController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("search_text", t)
                },
            )
        }

    // ---- TopBar config passed into the scaffold ----
    val topBar =
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

    // ---- Scaffold (drawer + top bar + nested nav graph) ----
    appScaffoldWithDrawer(
        navConfig =
            AppNavConfig(
                navController = drawerNavController,
                currentRoute = currentRoute,
            ),
        topBar = topBar,
        onLogout = onLogout,
    ) {
        // Nested navigation graph for drawer destinations
        NavHost(
            navController = drawerNavController,
            startDestination = Screen.GeneralPool.route,
        ) {
            composable(Screen.GeneralPool.route) { generalPoolScreen(drawerNavController) }
            composable(Screen.MyOrders.route) { myOrdersScreen(drawerNavController) }
            composable(Screen.OrdersHistory.route) { ordersHistoryScreen(drawerNavController) }
            composable(Screen.Notifications.route) { notificationScreen(drawerNavController) }
            composable(Screen.DeliveriesLog.route) { deliveriesLogScreen(drawerNavController) }
            composable(Screen.Settings.route) { settingsOptions(drawerNavController) }
            composable(Screen.MyPool.route) { myPoolScreen(drawerNavController) }
            composable(Screen.Chat.route) { chatScreen() }
        }
    }
}

// ---------- Route UI Spec ----------
private fun buildRouteUiSpec(
    currentRoute: String,
    nav: NavHostController,
): RouteUiSpec =
    when (currentRoute) {
        Screen.GeneralPool.route ->
            RouteUiSpec(
                title = "General Pool",
                showSearchIcon = true,
                searchPlaceholder = "Search by order no. or customer name",
                actionButtonLabel = "MY POOL",
                onActionButtonClick = { nav.navigateSingleTop(Screen.MyPool.route) },
            )

        Screen.MyPool.route ->
            RouteUiSpec(
                title = "My Pool",
                showSearchIcon = false,
                actionButtonLabel = "GENERAL POOL",
                onActionButtonClick = { nav.navigateSingleTop(Screen.GeneralPool.route) },
            )

        Screen.MyOrders.route ->
            RouteUiSpec(
                title = "My Orders",
                showSearchIcon = true,
                searchPlaceholder = "Search by order number",
            )

        Screen.DeliveriesLog.route ->
            RouteUiSpec(
                title = "Deliveries Log",
                showSearchIcon = true,
                searchPlaceholder = "Search by order number",
            )

        Screen.OrdersHistory.route ->
            RouteUiSpec(
                title = "Order History",
                showSearchIcon = false,
                actionIcon = Icons.Filled.MoreVert,
                onActionIconClick = { /* TODO show menu */ },
            )

        Screen.Notifications.route -> RouteUiSpec(title = "Notifications", showSearchIcon = false)
        Screen.Settings.route -> RouteUiSpec(title = "Settings", showSearchIcon = false)
        Screen.Chat.route -> RouteUiSpec(title = "Chat", showSearchIcon = false)
        else -> RouteUiSpec(title = "App", showSearchIcon = false)
    }
