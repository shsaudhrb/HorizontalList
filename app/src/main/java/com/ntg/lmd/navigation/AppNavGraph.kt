@file:Suppress("DEPRECATION")

package com.ntg.lmd.navigation

import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ntg.lmd.mainscreen.ui.screens.chatScreen
import com.ntg.lmd.mainscreen.ui.screens.deliveriesLogScreen
import com.ntg.lmd.mainscreen.ui.screens.generalPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.myOrdersScreen
import com.ntg.lmd.mainscreen.ui.screens.myPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.ordersHistoryScreen
import com.ntg.lmd.navigation.component.appScaffoldWithDrawer
import com.ntg.lmd.notification.ui.screens.notificationScreen
import com.ntg.lmd.notification.ui.viewmodel.DeepLinkViewModel
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
                openNotifications = argOpen || deepOpen, // ← single flag
            )
        }
    }
}

@Composable
private fun drawerHost(
    onLogout: () -> Unit,
    openNotifications: Boolean = false,
) {
    val drawerNavController = rememberNavController()
    val backStack by drawerNavController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.GeneralPool.route

    val innerStart = if (openNotifications) Screen.Notifications.route else Screen.GeneralPool.route

    LaunchedEffect(openNotifications) {
        if (openNotifications) {
            drawerNavController.navigate(Screen.Notifications.route) { launchSingleTop = true }
        }
    }

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

    appScaffoldWithDrawer(
        navController = drawerNavController,
        currentRoute = currentRoute,
        title = title,
        onLogout = onLogout,
    ) {
        NavHost(
            navController = drawerNavController,
            startDestination = innerStart,
        ) {
            composable(Screen.GeneralPool.route) { generalPoolScreen() }
            composable(Screen.MyOrders.route) { myOrdersScreen(drawerNavController) }
            composable(Screen.OrdersHistory.route) { ordersHistoryScreen(drawerNavController) }
            composable(
                route = Screen.Notifications.route,
                deepLinks = listOf(navDeepLink { uriPattern = "myapp://notifications" }),
            ) { notificationScreen() }
            composable(Screen.DeliveriesLog.route) { deliveriesLogScreen(drawerNavController) }
            composable(Screen.Settings.route) { settingsOptions(drawerNavController) }
            composable(Screen.MyPool.route) { myPoolScreen(drawerNavController) }
            composable(Screen.Chat.route) { chatScreen() }
        }
    }
}
