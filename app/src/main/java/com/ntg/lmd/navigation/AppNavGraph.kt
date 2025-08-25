package com.ntg.lmd.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.ui.screens.chatScreen
import com.ntg.lmd.mainscreen.ui.screens.deliveriesLogScreen
import com.ntg.lmd.mainscreen.ui.screens.generalPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.myOrdersScreen
import com.ntg.lmd.mainscreen.ui.screens.myPoolScreen
import com.ntg.lmd.navigation.component.AppScaffoldActions
import com.ntg.lmd.navigation.component.AppScaffoldConfig
import com.ntg.lmd.navigation.component.appScaffoldWithDrawer
import com.ntg.lmd.navigation.component.navigateSingleTop
import com.ntg.lmd.notification.ui.screens.notificationScreen
import com.ntg.lmd.order.ui.screen.ordersHistoryRoute
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
            LoginScreen(navController = rootNavController)
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

    val openOrdersMenu = remember { mutableStateOf<(() -> Unit)?>(null) }

    val titleRes =
        when (currentRoute) {
            Screen.GeneralPool.route -> R.string.menu_general_pool
            Screen.MyOrders.route -> R.string.menu_my_orders
            Screen.OrdersHistory.route -> R.string.menu_order_history
            Screen.Notifications.route -> R.string.menu_notifications
            Screen.DeliveriesLog.route -> R.string.menu_deliveries_log
            Screen.Settings.route -> R.string.menu_settings
            Screen.Chat.route -> R.string.menu_chat
            else -> R.string.app_name
        }
    val title = stringResource(titleRes)

    appScaffoldWithDrawer(
        config =
            AppScaffoldConfig(
                currentRoute = currentRoute,
                title = title,
                showOrdersMenu = currentRoute == Screen.OrdersHistory.route,
            ),
        actions =
            AppScaffoldActions(
                onNavigate = { route -> drawerNavController.navigateSingleTop(route) },
                onLogout = onLogout,
                onOrdersMenuClick = openOrdersMenu.value,
            ),
    ) {
        // ⬇️ The drawer's content lives inside the scaffold content slot
        NavHost(
            navController = drawerNavController,
            startDestination = Screen.GeneralPool.route,
        ) {
            composable(Screen.GeneralPool.route) { generalPoolScreen() }
            composable(Screen.MyOrders.route) { myOrdersScreen(drawerNavController) }
            composable(Screen.OrdersHistory.route) {
                ordersHistoryRoute(
                    registerOpenMenu = { opener -> openOrdersMenu.value = opener },
                )
            }
            composable(Screen.Notifications.route) { notificationScreen(drawerNavController) }
            composable(Screen.DeliveriesLog.route) { deliveriesLogScreen(drawerNavController) }
            composable(Screen.Settings.route) { settingsOptions(drawerNavController) }
            composable(Screen.MyPool.route) { myPoolScreen(drawerNavController) }
            composable(Screen.Chat.route) { chatScreen() }
        }
    }
}
