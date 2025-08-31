package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    // Do not draw anything if not enabled (no location permission)
    if (!enabled) return

    val value = maxDistanceKm.coerceIn(DISTANCE_MIN_KM, DISTANCE_MAX_KM)

    Surface(
        modifier =
            Modifier
                .width(280.dp)
                .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val context = LocalContext.current
            Text(
                text = "${value.roundToInt()} ${context.getString(R.string.kilometer)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            // Straight track + circular thumb
            circleSlider(
                value = value,
                onValueChange = { onMaxDistanceKm(it.coerceIn(DISTANCE_MIN_KM, DISTANCE_MAX_KM)) },
                valueRange = DISTANCE_MIN_KM..DISTANCE_MAX_KM,
                enabled = enabled,
            )
        }
    }
}

@Composable
fun circleSlider(
    value: Double,
    onValueChange: (Double) -> Unit,
    valueRange: ClosedFloatingPointRange<Double> = DISTANCE_MIN_KM..DISTANCE_MAX_KM,
    enabled: Boolean = true,
) {
    val trackWidth = 220.dp
    val thumbRadius = 10.dp

    // Capture theme colors in composable scope (OK to read MaterialTheme here)
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val thumbColor: Color = MaterialTheme.colorScheme.primary

    Box(
        modifier =
            Modifier
                .width(trackWidth)
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Thin straight line
        Box(
            modifier =
                Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(trackColor),
        )

        // Draggable circular thumb
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .then(
                        if (enabled) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val posX = change.position.x.coerceIn(0f, size.width.toFloat())
                                    val fraction = posX / size.width // Float 0..1
                                    val newValue =
                                        valueRange.start +
                                            (
                                                fraction.toDouble() *
                                                    (valueRange.endInclusive - valueRange.start)
                                            )
                                    onValueChange(newValue)
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
        ) {
            val fraction =
                (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val x = size.width * fraction.toFloat()
            drawCircle(
                color = thumbColor,
                radius = thumbRadius.toPx(),
                center = Offset(x, size.height / 2),
            )
        }
    }
}
