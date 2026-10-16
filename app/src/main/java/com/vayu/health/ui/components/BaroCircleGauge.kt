package com.vayu.health.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Baro 3-gauge hero circular component.
 * Displays Sleep, Readiness, or Strain gauges matching Baro's exact geometry:
 * - 270-degree arc opening at bottom (start 135 deg, sweep 270 deg)
 * - Dotted/dashed circle option for calibrating/baseline states
 * - Center value and bottom uppercase label
 */
@Composable
fun BaroCircleGauge(
    label: String,
    valueText: String,
    progress: Float?, // 0.0 to 1.0; null indicates dashed/calibrating state
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = Color(0xFF202024),
    showNeedleTick: Boolean = false
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        val target = (progress ?: 0f).coerceIn(0f, 1f)
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
                val strokePx = strokeWidth.toPx()
                val diameter = this.size.minDimension
                val arcSize = Size(diameter, diameter)
                val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)

                if (progress == null) {
                    // Calibrating / Baseline building mode: Dashed radial circle
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    drawArc(
                        color = accentColor.copy(alpha = 0.55f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx * 0.75f, pathEffect = dashPathEffect)
                    )
                } else {
                    // Normal 270-degree arc
                    val startAngle = 135f
                    val maxSweep = 270f

                    // Background Track
                    drawArc(
                        color = trackColor,
                        startAngle = startAngle,
                        sweepAngle = maxSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    // Active Arc
                    val currentSweep = maxSweep * animatedProgress.value
                    if (currentSweep > 0.5f) {
                        drawArc(
                            color = accentColor,
                            startAngle = startAngle,
                            sweepAngle = currentSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }

                    // Optional needle tick marker at head of arc (used in Strain)
                    if (showNeedleTick && animatedProgress.value > 0.05f) {
                        val angleRad = Math.toRadians((startAngle + currentSweep).toDouble())
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val radius = diameter / 2f
                        val innerRadius = radius - strokePx * 0.9f
                        val outerRadius = radius + strokePx * 0.9f

                        val tickStart = Offset(
                            x = (center.x + innerRadius * cos(angleRad)).toFloat(),
                            y = (center.y + innerRadius * sin(angleRad)).toFloat()
                        )
                        val tickEnd = Offset(
                            x = (center.x + outerRadius * cos(angleRad)).toFloat(),
                            y = (center.y + outerRadius * sin(angleRad)).toFloat()
                        )
                        drawLine(
                            color = Color.White,
                            start = tickStart,
                            end = tickEnd,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Center Value
            Text(
                text = valueText,
                fontSize = if (valueText.length > 3) 20.sp else 23.sp,
                fontWeight = FontWeight.Bold,
                color = BaroTextWhite
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Uppercase Label
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = BaroTextSecondary,
            letterSpacing = 1.sp
        )
    }
}
