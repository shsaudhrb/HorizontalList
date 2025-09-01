package com.ntg.lmd.navigation.component

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
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
    onOpenOrderDetails: (String) -> Unit,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        addGeneralPool(navController)
        addMyOrders( onOpenOrderDetails)
        addOrderDetails(navController)
        addNotifications()
        addOrdersHistory(registerOpenMenu)
        addDeliveriesLog(navController)
        addSettings()
        addMyPool(onOpenOrderDetails)
        addChat()
    }
}

// ------------ Extracted routes (logic unchanged) ------------

private fun NavGraphBuilder.addGeneralPool(navController: NavHostController) {
    composable(Screen.GeneralPool.route) { generalPoolScreen(navController) }
}

private fun NavGraphBuilder.addMyOrders(
    onOpenOrderDetails: (String) -> Unit,
) {
    composable(Screen.MyOrders.route) {
        myOrdersScreen(
            onOpenOrderDetails = onOpenOrderDetails,
        )
    }
}

private fun NavGraphBuilder.addOrderDetails(navController: NavHostController) {
    composable(
        route = "order/{id}",
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { backStackEntry ->
        val idStr = backStackEntry.arguments?.getString("id")
        val id = idStr
        orderDetailsScreen(orderId = id, navController = navController)
    }
}

private fun NavGraphBuilder.addNotifications() {
    composable(
        route = Screen.Notifications.route,
        deepLinks = listOf(navDeepLink { uriPattern = "myapp://notifications" }),
    ) { notificationScreen() }
}

private fun NavGraphBuilder.addOrdersHistory(registerOpenMenu: (setter: (() -> Unit)?) -> Unit) {
    composable(Screen.OrdersHistory.route) {
        ordersHistoryRoute(registerOpenMenu = registerOpenMenu)
    }
}

private fun NavGraphBuilder.addDeliveriesLog(navController: NavHostController) {
    composable(Screen.DeliveriesLog.route) { deliveriesLogScreen(navController) }
}

private fun NavGraphBuilder.addSettings() {
    composable(Screen.Settings.route) { entry -> settingsScreen(entry) }
}

private fun NavGraphBuilder.addMyPool(onOpenOrderDetails: (String) -> Unit) {
    composable(Screen.MyPool.route) {
        myPoolScreen(onOpenOrderDetails = onOpenOrderDetails)
    }
}

private fun NavGraphBuilder.addChat() {
    composable(Screen.Chat.route) { chatScreen() }
}