package com.ntg.lmd.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ntg.lmd.authentication.ui.screens.login.loginScreen
import com.ntg.lmd.authentication.ui.screens.register.registerScreen
import com.ntg.lmd.authentication.ui.screens.splash.splashScreen
import com.ntg.lmd.mainscreen.ui.screens.deliveriesLogScreen
import com.ntg.lmd.mainscreen.ui.screens.generalPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.myOrdersScreen
import com.ntg.lmd.mainscreen.ui.screens.myPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.ordersHistoryScreen
import com.ntg.lmd.navigation.component.appScaffoldWithDrawer
import com.ntg.lmd.notification.ui.screens.notificationScreen
import com.ntg.lmd.settings.ui.screens.settingsOptions
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ntg.lmd.mainscreen.ui.screens.chatScreen

@Composable
fun appNavGraph(rootNavController: NavHostController) {
    NavHost(
        navController = rootNavController,
        startDestination = Screen.Splash.route,
    ) {
        // ---------- Splash ----------
        composable(Screen.Splash.route) {
            splashScreen(
                navController = rootNavController,
                onDecide = { isLoggedIn ->
                    rootNavController.navigate(
                        if (isLoggedIn) Screen.Drawer.route else Screen.Login.route
                    ) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ---------- Auth ----------
        composable(Screen.Login.route) {
            loginScreen(
                navController = rootNavController,
                // inside loginScreen's onClick -> navController.navigate(Screen.Drawer.route) { popUpTo(Screen.Login.route){ inclusive = true } }
            )
        }
        composable(Screen.Register.route) {
            registerScreen(
                navController = rootNavController,
                // after register success, navigate to Drawer similarly
            )
        }

        // ---------- Drawer Host (AFTER LOGIN) ----------
        composable(Screen.Drawer.route) {
            DrawerHost(
                onLogout = {
                    // clear tokens, then go back to login
                    rootNavController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Drawer.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
private fun DrawerHost(onLogout: () -> Unit) {
    val drawerNavController = rememberNavController()
    val backStack by drawerNavController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.GeneralPool.route

    val title = when (currentRoute) {
        Screen.GeneralPool.route   -> "General Pool"
        Screen.MyOrders.route      -> "My Orders"
        Screen.OrdersHistory.route -> "Order History"
        Screen.Notifications.route -> "Notifications"
        Screen.DeliveriesLog.route -> "Deliveries Log"
        Screen.Settings.route      -> "Settings"
        Screen.Chat.route          -> "Chat"
        Screen.MyPool.route        -> "My Pool"
        else                       -> "App"
    }

    appScaffoldWithDrawer(
        navController = drawerNavController, // inner controller
        currentRoute = currentRoute,
        title = title,
        onLogout = onLogout
    ) {
        NavHost(
            navController = drawerNavController,       // inner NavHost uses inner controller
            startDestination = Screen.GeneralPool.route
        ) {
            composable(Screen.GeneralPool.route)   { generalPoolScreen(drawerNavController) }
            composable(Screen.MyOrders.route)      { myOrdersScreen(drawerNavController) }
            composable(Screen.OrdersHistory.route) { ordersHistoryScreen(drawerNavController) }
            composable(Screen.Notifications.route) { notificationScreen(drawerNavController) }
            composable(Screen.DeliveriesLog.route) { deliveriesLogScreen(drawerNavController) }
            composable(Screen.Settings.route)      { settingsOptions(drawerNavController) }
            composable(Screen.MyPool.route)        { myPoolScreen(drawerNavController) }
            composable(Screen.Chat.route)       { chatScreen(drawerNavController) }
        }
    }
}
