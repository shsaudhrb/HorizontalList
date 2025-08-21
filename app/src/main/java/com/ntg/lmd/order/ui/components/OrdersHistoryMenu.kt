package com.ntg.lmd.order.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
fun ordersHistoryMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onFilter: () -> Unit,
    onSort: () -> Unit,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(Modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .align(if (isRtl) Alignment.TopStart else Alignment.TopEnd)
                    .size(dimensionResource(R.dimen.hairline)),
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismiss,
                offset = DpOffset(0.dp, dimensionResource(R.dimen.menu_offset_y)),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.export_pdf)) },
                    leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) },
                    onClick = {
                        onDismiss()
                        onExportPdf()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.filter)) },
                    leadingIcon = { Icon(Icons.Outlined.FilterList, null) },
                    onClick = {
                        onDismiss()
                        onFilter()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort)) },
                    leadingIcon = { Icon(Icons.Outlined.Sort, null) },
                    onClick = {
                        onDismiss()
                        onSort()
                    },
                )
            }
        }
    }
}
