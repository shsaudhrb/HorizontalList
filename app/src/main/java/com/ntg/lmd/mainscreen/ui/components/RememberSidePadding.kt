package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R

@Composable
fun rememberSidePadding(): Dp {
    val cardWidth = dimensionResource(R.dimen.myOrders_card_width)
    val screen = LocalConfiguration.current.screenWidthDp.dp
    return ((screen - cardWidth) / 2).coerceAtLeast(0.dp)
}
