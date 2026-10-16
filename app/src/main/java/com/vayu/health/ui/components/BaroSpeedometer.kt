package com.vayu.health.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vayu.health.ui.theme.BaroHeartCoral
import com.vayu.health.ui.theme.BaroReadinessTeal
import com.vayu.health.ui.theme.BaroZone3Orange
import kotlin.math.cos
import kotlin.math.sin

/**
 * Semi-circle radial speedometer for the Baro Stress tile.
 * Features radial tick marks radiating around a 180-degree semi-circle arc,
 * with color zones (Teal Calm -> Orange Moderate -> Coral High) and an active indicator needle.
 */
@Composable
fun BaroSpeedometer(
    score: Float?, // Typical stress score e.g. 0.0 to 3.0 or 0f to 1f normalized; null = unrecorded
    modifier: Modifier = Modifier,
    maxScore: Float = 3.0f,
    height: Dp = 52.dp
) {
    val hasScore = score != null
    val normalized = if (hasScore) (score!! / maxScore).coerceIn(0f, 1f) else 0f
    val animatedNormalized = remember { Animatable(0f) }

    LaunchedEffect(normalized) {
        animatedNormalized.animateTo(
            targetValue = normalized,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h - 4.dp.toPx()
        val radius = (h - 8.dp.toPx()).coerceAtLeast(20.dp.toPx())

        val tickCount = 21
        val startAngleDeg = 180f
        val totalSweepDeg = 180f
        val angleStep = totalSweepDeg / (tickCount - 1)

        val innerRadius = radius - 10.dp.toPx()
        val outerRadius = radius

        for (i in 0 until tickCount) {
            val angleDeg = startAngleDeg + i * angleStep
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val fraction = i / (tickCount - 1).toFloat()
            val baseColor = when {
                fraction < 0.35f -> BaroReadinessTeal
                fraction < 0.70f -> BaroZone3Orange
                else -> BaroHeartCoral
            }

            val isActive = hasScore && (fraction <= animatedNormalized.value)
            val tickColor = if (isActive) baseColor else Color(0xFF26262C)

            val start = Offset(
                x = (centerX + innerRadius * cos(angleRad)).toFloat(),
                y = (centerY + innerRadius * sin(angleRad)).toFloat()
            )
            val end = Offset(
                x = (centerX + outerRadius * cos(angleRad)).toFloat(),
                y = (centerY + outerRadius * sin(angleRad)).toFloat()
            )

            drawLine(
                color = tickColor,
                start = start,
                end = end,
                strokeWidth = if (isActive) 2.5.dp.toPx() else 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw indicator needle or center hub only if score is recorded
        if (hasScore) {
            val needleAngleDeg = startAngleDeg + animatedNormalized.value * totalSweepDeg
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLength = radius - 3.dp.toPx()

            val needleTip = Offset(
                x = (centerX + needleLength * cos(needleAngleRad)).toFloat(),
                y = (centerY + needleLength * sin(needleAngleRad)).toFloat()
            )

            // Center pivot dot
            drawCircle(
                color = BaroReadinessTeal,
                radius = 3.dp.toPx(),
                center = Offset(centerX, centerY)
            )

            // Needle line
            drawLine(
                color = Color.White,
                start = Offset(centerX, centerY),
                end = needleTip,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        } else {
            // Subtle center neutral dot
            drawCircle(
                color = Color(0xFF323238),
                radius = 2.5.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }
    }
}
