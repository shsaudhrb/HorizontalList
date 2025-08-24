package com.ntg.lmd.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ntg.lmd.mainscreen.ui.screens.chatScreen
import com.ntg.lmd.mainscreen.ui.screens.deliveriesLogScreen
import com.ntg.lmd.mainscreen.ui.screens.generalPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.myOrdersScreen
import com.ntg.lmd.mainscreen.ui.screens.myPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.ordersHistoryScreen
import com.ntg.lmd.navigation.component.AppBarConfig
import com.ntg.lmd.navigation.component.appScaffoldWithDrawer
import com.ntg.lmd.notification.ui.screens.notificationScreen
import com.ntg.lmd.settings.ui.screens.settingsOptions
import com.ntg.lmd.authentication.ui.screens.login.loginScreen as LoginScreen
import com.ntg.lmd.authentication.ui.screens.register.registerScreen as RegisterScreen
import com.ntg.lmd.authentication.ui.screens.splash.splashScreen as SplashScreen

@Composable
fun appNavGraph(rootNavController: NavHostController) {
    NavHost(
        navController = rootNavController,
        startDestination = Screen.Splash.route,
    ) {
        // ---------- Splash ----------
        composable(Screen.Splash.route) {
            SplashScreen(
                navController = rootNavController,
                onDecide = { isLoggedIn ->
                    rootNavController.navigate(
                        if (isLoggedIn) Screen.Drawer.route else Screen.Login.route,
                    ) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        // ---------- Auth ----------
        composable(Screen.Login.route) {
            LoginScreen()
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                navController = rootNavController,
                // after register success, navigate to Drawer similarly
            )
        }

        // ---------- Drawer Host (AFTER LOGIN) ----------
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

    val title =
        when (currentRoute) {
            Screen.GeneralPool.route -> "General Pool"
            Screen.MyOrders.route -> "My Orders"
            Screen.OrdersHistory.route -> "Order History"
            Screen.Notifications.route -> "Notifications"
            Screen.DeliveriesLog.route -> "Deliveries Log"
            Screen.Settings.route -> "Settings"
            Screen.Chat.route -> "Chat"
            Screen.MyPool.route -> "My Pool"
            else -> "App"
        }

    val showSearch = currentRoute == Screen.MyOrders.route // appear the search in My order screen
    var search by rememberSaveable { mutableStateOf("") }

    appScaffoldWithDrawer(
        navController = drawerNavController,
        currentRoute = currentRoute,
        appBar =
            AppBarConfig(
                title = title,
                showSearch = showSearch,
                searchValue = search,
                onSearchChange = { text -> search = text },
            ),
        onLogout = onLogout,
    ) {
        NavHost(
            navController = drawerNavController,
            startDestination = Screen.GeneralPool.route,
        ) {
            composable(Screen.GeneralPool.route) { generalPoolScreen() }
            composable(Screen.MyOrders.route) {
                myOrdersScreen(
                    navController = drawerNavController,
                    externalQuery = search,
                )
            }
            composable(Screen.OrdersHistory.route) { ordersHistoryScreen(drawerNavController) }
            composable(Screen.Notifications.route) { notificationScreen(drawerNavController) }
            composable(Screen.DeliveriesLog.route) { deliveriesLogScreen(drawerNavController) }
            composable(Screen.Settings.route) { settingsOptions(drawerNavController) }
            composable(Screen.MyPool.route) { myPoolScreen(drawerNavController) }
            composable(Screen.Chat.route) { chatScreen() }
        }
    }
}
