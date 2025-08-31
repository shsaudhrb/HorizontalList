package com.ntg.lmd.navigation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ntg.lmd.R
import com.ntg.lmd.navigation.RouteUiSpec
import com.ntg.lmd.navigation.Screen

@Composable
fun buildRouteUiSpec(
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
