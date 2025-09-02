package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import kotlin.math.roundToInt

// Slider constraints
private const val DISTANCE_MIN_KM: Double = 1.0
private const val DISTANCE_MAX_KM: Double = 100.0

@Composable
fun distanceFilterBar(
    maxDistanceKm: Double,
    onMaxDistanceKm: (Double) -> Unit,
    enabled: Boolean,
) {
    if (!enabled) return

    val value = maxDistanceKm.coerceIn(DISTANCE_MIN_KM, DISTANCE_MAX_KM)

    distanceCard {
        distanceValueLabel(value)
        distanceCircleSlider(
            value = value,
            onValueChange = { onMaxDistanceKm(it.coerceIn(DISTANCE_MIN_KM, DISTANCE_MAX_KM)) },
            enabled = enabled,
        )
    }
}

@Composable
private fun distanceCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.width(280.dp).padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun distanceValueLabel(value: Double) {
    val context = LocalContext.current
    Text(
        text = "${value.roundToInt()} ${context.getString(R.string.kilometer)}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun distanceCircleSlider(
    value: Double,
    onValueChange: (Double) -> Unit,
    enabled: Boolean,
) {
    circleSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = DISTANCE_MIN_KM..DISTANCE_MAX_KM,
        enabled = enabled,
    )
}

private data class SliderStyle(
    val trackWidth: Dp,
    val thumbRadius: Dp,
    val trackColor: Color,
    val thumbColor: Color,
)

@Composable
private fun rememberSliderStyle(): SliderStyle {
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val thumbColor = MaterialTheme.colorScheme.primary
    return remember(trackColor, thumbColor) {
        SliderStyle(
            trackWidth = 220.dp,
            thumbRadius = 10.dp,
            trackColor = trackColor,
            thumbColor = thumbColor,
        )
    }
}

@Composable
fun circleSlider(
    value: Double,
    onValueChange: (Double) -> Unit,
    valueRange: ClosedFloatingPointRange<Double> = DISTANCE_MIN_KM..DISTANCE_MAX_KM,
    enabled: Boolean = true,
) {
    val style = rememberSliderStyle()
    val dragMod = dragModifier(enabled, valueRange, onValueChange)

    Box(
        modifier = Modifier.width(style.trackWidth).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        sliderTrack(style.trackColor)
        Canvas(modifier = Modifier.matchParentSize().then(dragMod)) {
            val fraction =
                (
                    (value - valueRange.start) /
                        (valueRange.endInclusive - valueRange.start)
                ).toFloat().coerceIn(0f, 1f)
            val x = size.width * fraction
            drawCircle(
                color = style.thumbColor,
                radius = style.thumbRadius.toPx(),
                center = Offset(x, size.height / 2),
            )
        }
    }
}

@Composable
private fun sliderTrack(color: Color) {
    Box(
        modifier =
            Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(color),
    )
}

@Composable
private fun dragModifier(
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Double>,
    onValueChange: (Double) -> Unit,
): Modifier {
    if (!enabled) return Modifier
    return Modifier.pointerInput(Unit) {
        detectDragGestures { change, _ ->
            change.consume()
            val posX = change.position.x.coerceIn(0f, size.width.toFloat())
            val fraction = posX / size.width
            val newValue =
                valueRange.start +
                    (fraction.toDouble() * (valueRange.endInclusive - valueRange.start))
            onValueChange(newValue)
        }
    }
}
