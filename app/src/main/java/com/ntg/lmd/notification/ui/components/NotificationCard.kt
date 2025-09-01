package com.ntg.lmd.notification.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import com.ntg.lmd.R
import com.ntg.lmd.notification.domain.model.AgentNotification
import com.ntg.lmd.notification.ui.model.NotificationUi
import com.ntg.lmd.notification.ui.model.relativeAgeLabel

private const val MILLIS_PER_MINUTE = 60_000L

private data class NotificationVisuals(
    val accent: Color,
    val icon: ImageVector,
)

@Composable
internal fun notificationCard(item: NotificationUi) {
    val nowMs = rememberNowMillis(MILLIS_PER_MINUTE)
    val ageLabel = relativeAgeLabel(nowMs, item.timestampMs)
    val visuals = notificationVisuals(item)
    notificationCardShell {
        notificationRow(visuals, item.message, ageLabel)
    }
}

@Composable
private fun notificationCardShell(content: @Composable RowScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.cardRoundCorner)),
        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = dimensionResource(R.dimen.smallElevation),
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(dimensionResource(R.dimen.smallSpace)),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun notificationVisuals(item: NotificationUi): NotificationVisuals {
    val accent =
        when (item.type) {
            AgentNotification.Type.ORDER_STATUS -> MaterialTheme.colorScheme.primary
            AgentNotification.Type.WALLET -> MaterialTheme.colorScheme.tertiary
            AgentNotification.Type.OTHER -> MaterialTheme.colorScheme.secondary
        }
    val icon =
        when (item.type) {
            AgentNotification.Type.ORDER_STATUS -> Icons.Outlined.LocalShipping
            AgentNotification.Type.WALLET -> Icons.Outlined.AttachMoney
            AgentNotification.Type.OTHER -> Icons.Outlined.Notifications
        }
    return NotificationVisuals(accent, icon)
}

@Composable
private fun RowScope.notificationRow(
    visuals: NotificationVisuals,
    message: String,
    ageLabel: String,
) {
    leadingIcon(visuals)
    Spacer(Modifier.width(dimensionResource(R.dimen.smallSpace)))
    messageAndAgeColumn(message, ageLabel)
    accentBar(visuals)
}

@Composable
private fun leadingIcon(visuals: NotificationVisuals) {
    Box(
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.notificationIconBox))
                .background(visuals.accent.copy(alpha = 0.15f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(visuals.icon, contentDescription = null, tint = visuals.accent)
    }
}

@Composable
private fun RowScope.messageAndAgeColumn(
    message: String,
    ageLabel: String,
) {
    Column(Modifier.weight(1f)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.tinySpace)))
        Text(
            text = ageLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun accentBar(visuals: NotificationVisuals) {
    Spacer(
        Modifier
            .width(dimensionResource(R.dimen.extraSmallSpace))
            .height(IntrinsicSize.Max)
            .background(
                visuals.accent.copy(alpha = 0.8f),
                RoundedCornerShape(
                    topStart = dimensionResource(R.dimen.notificationAccentBarRadius),
                    bottomStart = dimensionResource(R.dimen.notificationAccentBarRadius),
                ),
            ),
    )
}
