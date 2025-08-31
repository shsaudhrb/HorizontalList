package com.ntg.lmd.navigation.component

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ntg.lmd.mainscreen.ui.screens.chatScreen
import com.ntg.lmd.mainscreen.ui.screens.deliveriesLogScreen
import com.ntg.lmd.mainscreen.ui.screens.generalPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.myOrdersScreen
import com.ntg.lmd.mainscreen.ui.screens.myPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.orderDetailsScreen
import com.ntg.lmd.navigation.Screen
import com.ntg.lmd.notification.ui.screens.notificationScreen
import com.ntg.lmd.order.ui.screen.ordersHistoryRoute
import com.ntg.lmd.settings.ui.screens.settingsScreen

@Composable
fun drawerNavGraph(
    navController: NavHostController,
    startDestination: String,
    registerOpenMenu: (setter: (() -> Unit)?) -> Unit,
    externalQuery: String,
    onOpenOrderDetails: (Long) -> Unit,
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
            val id = idStr?.toLongOrNull()
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
        composable(Screen.Settings.route) { entry ->
            settingsScreen(entry)
        }
        composable(Screen.MyPool.route) { myPoolScreen() }
        composable(Screen.Chat.route) { chatScreen() }
    }
}
