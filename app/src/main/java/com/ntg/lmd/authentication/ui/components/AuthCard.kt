package com.ntg.lmd.authentication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R

// Card with animation
@Composable
fun authCard(
    cardScale: Float,
    cardElevation: Float,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.cardRoundCorner))
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                }.shadow(cardElevation.dp, shape, clip = false)
                .clip(shape),
        shape = shape,
        border =
            BorderStroke(
                dimensionResource(R.dimen.smallestStrokeWidth),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.padding(dimensionResource(R.dimen.largeSpace))) { content() }
    }
}
