package com.ntg.lmd.navigation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ntg.lmd.R
import com.ntg.lmd.navigation.Screen
import com.ntg.lmd.ui.theme.CupertinoCellBackground
import com.ntg.lmd.ui.theme.CupertinoLabelPrimary
import com.ntg.lmd.ui.theme.CupertinoLabelSecondary

@Composable
fun drawerItemRow(
    entry: DrawerItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val textColor = if (entry.enabled) CupertinoLabelPrimary else CupertinoLabelSecondary
    val iconAlpha = if (entry.enabled) ENABLED_ICON else DISABLED_ICON
    val label = stringResource(entry.labelRes)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = entry.enabled, onClick = onClick)
                .padding(
                    horizontal = dimensionResource(R.dimen.mediumSpace),
                    vertical = dimensionResource(R.dimen.smallSpace),
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        drawerItemIcon(entry, textColor, iconAlpha)
        drawerItemText(label, textColor)
        drawerItemBadge(entry)
        drawerItemArrow(entry)
    }

    if (selected) {
        HorizontalDivider(
            thickness = dimensionResource(R.dimen.hairline),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        )
    }
}

@Composable
private fun drawerItemIcon(
    entry: DrawerItem,
    tint: Color,
    alpha: Float,
) {
    Icon(
        imageVector = entry.icon,
        contentDescription = null,
        tint = tint,
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.drawer_icon_size))
                .graphicsLayer(alpha = alpha),
    )
    Spacer(Modifier.width(dimensionResource(R.dimen.smallSpace)))
}

@Composable
private fun RowScope.drawerItemText(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        color = color,
        fontSize = dimensionResource(R.dimen.drawer_item_text_size).value.sp,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun drawerItemBadge(entry: DrawerItem) {
    entry.badgeCount?.let {
        Text(
            text = it.toString(),
            color = CupertinoLabelSecondary,
            fontSize = dimensionResource(R.dimen.drawer_badge_text_size).value.sp,
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.smallerSpace)))
    }
}

@Composable
private fun drawerItemArrow(entry: DrawerItem) {
    if (entry.enabled && entry.route != Screen.Logout.route) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = CupertinoLabelSecondary,
        )
    }
}

@Composable
fun drawerHeader(name: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .height(dimensionResource(R.dimen.drawer_header_height))
                .padding(
                    horizontal = dimensionResource(R.dimen.mediumSpace),
                    vertical = dimensionResource(R.dimen.mediumSpace),
                ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_user_placeholder),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(dimensionResource(R.dimen.drawer_avatar_size))
                        .clip(CircleShape),
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.smallSpace)))
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(R.dimen.drawer_header_text_size).value.sp,
            )
        }
    }
}

@Composable
fun groupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .padding(horizontal = dimensionResource(R.dimen.smallSpace))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.card_radius)))
                .background(CupertinoCellBackground)
                .padding(vertical = dimensionResource(R.dimen.smallSpace)),
        content = content,
    )
}
