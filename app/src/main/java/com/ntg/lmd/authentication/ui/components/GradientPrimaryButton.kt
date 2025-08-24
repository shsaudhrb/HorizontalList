package com.ntg.lmd.authentication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import com.ntg.lmd.R

@Composable
fun gradientPrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.buttonRoundCorner))
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.extraSmallSpace))
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary)
                .height(dimensionResource(R.dimen.buttonHeight))
                .clickable(
                    enabled = !loading,
                    interactionSource = interaction,
                    indication = null,
                ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = dimensionResource(R.dimen.smallStrokeWidth),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(dimensionResource(R.dimen.buttonProgressIndicatorSize)),
            )
        } else {
            Text(text = text, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
        }
    }
}
