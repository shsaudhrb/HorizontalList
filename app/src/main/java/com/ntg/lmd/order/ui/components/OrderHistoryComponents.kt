package com.ntg.lmd.order.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R

@Composable
fun loadingFooter() {
    Row(
        Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.smallerSpace)),
        horizontalArrangement = Arrangement.Center,
    ) { CircularProgressIndicator() }
}

@Composable
fun endFooter() {
    Box(
        Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.smallSpace)),
        contentAlignment = Alignment.Center,
    ) { Text("• End of list •") }
}

@Composable
fun statusBadge(
    text: String,
    color: Color,
) {
    Box(
        modifier = Modifier.padding(start = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
    }
}
