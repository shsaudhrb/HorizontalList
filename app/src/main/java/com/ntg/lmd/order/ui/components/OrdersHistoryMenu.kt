package com.ntg.lmd.order.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R

@Composable
private fun menuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    androidx.compose.material3.DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, null) },
        onClick = onClick,
    )
}

@Composable
private fun menuContent(
    onClose: () -> Unit,
    onExportPdfClick: () -> Unit,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
) {
    menuItem(stringResource(R.string.export_pdf), Icons.Default.PictureAsPdf) {
        onClose()
        onExportPdfClick()
    }
    menuItem(stringResource(R.string.filter), Icons.Default.FilterList) {
        onClose()
        onFilterClick()
    }
    menuItem(stringResource(R.string.sort), Icons.Default.Sort) {
        onClose()
        onSortClick()
    }
}

@Composable
fun ordersMenuDropdown(
    open: Boolean,
    onClose: () -> Unit,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    onExportPdfClick: () -> Unit,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val menuWidth = dimensionResource(R.dimen.menu_width)
    val menuRadius = dimensionResource(R.dimen.menu_corner_radius)
    val menuOffsetY = dimensionResource(R.dimen.menu_offset_y)
    val elev = dimensionResource(R.dimen.elevation_small)

    Box(Modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .align(if (isRtl) Alignment.TopStart else Alignment.TopEnd)
                    .size(dimensionResource(R.dimen.hairline)),
        ) {
            DropdownMenu(
                expanded = open,
                onDismissRequest = onClose,
                shape = RoundedCornerShape(menuRadius),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = elev,
                modifier = Modifier.width(menuWidth),
                offset = DpOffset(0.dp, menuOffsetY),
            ) {
                menuContent(onClose, onExportPdfClick, onFilterClick, onSortClick)
            }
        }
    }
}

@Composable
fun ordersHistoryMenu(
    open: Boolean,
    onClose: () -> Unit,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    onExportPdfClick: () -> Unit,
) {
    ordersMenuDropdown(open, onClose, onFilterClick, onSortClick, onExportPdfClick)
}
