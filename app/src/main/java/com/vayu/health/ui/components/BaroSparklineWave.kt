package com.vayu.health.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vayu.health.ui.theme.BaroHeartCoral

/**
 * Smooth cubic Bézier sparkline curve for heart rate tracking.
 * Features:
 * - Downsampling filter to eliminate raw sensor noise/grass spikes
 * - Subtle gradient fill underneath (alpha 0.16 to transparent)
 * - Animated live pulse indicator dot with expanding radar ring at latest reading
 * - Graceful baseline dashed line when no samples exist
 */
@Composable
fun BaroSparklineWave(
    samples: List<Long>,
    modifier: Modifier = Modifier,
    lineColor: Color = BaroHeartCoral,
    strokeWidth: Dp = 2.dp,
    showFill: Boolean = true,
    showLiveIndicator: Boolean = true,
    height: Dp = 48.dp
) {
    // Pulse animation for live tracking dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height

        if (samples.isEmpty()) {
            // Subtle dashed empty baseline
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            drawLine(
                color = Color(0x25FFFFFF),
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
            return@Canvas
        }

        // Downsample to ~40 smoothed points to eliminate jagged sensor grass
        val smoothed = downsampleSamples(samples, targetCount = 45)

        val values = if (smoothed.size == 1) listOf(smoothed[0], smoothed[0]) else smoothed
        val minVal = (values.minOrNull() ?: 50L).toFloat()
        val maxVal = (values.maxOrNull() ?: 150L).toFloat()
        val range = (maxVal - minVal).coerceAtLeast(15f)

        val padY = 8.dp.toPx()
        val usableH = h - padY * 2
        val stepX = w / (values.size - 1).coerceAtLeast(1)

        val points = values.mapIndexed { index, v ->
            val norm = (v - minVal) / range
            val y = h - padY - (norm * usableH)
            val x = index * stepX
            Offset(x, y)
        }

        // Smooth cubic Bézier path
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }

        // Fill under curve
        if (showFill) {
            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, h)
                lineTo(points.first().x, h)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    startY = padY,
                    endY = h
                )
            )
        }

        // Stroke line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )

        // Live tracking indicator dot at the latest sample point
        if (showLiveIndicator && points.isNotEmpty()) {
            val lastPoint = points.last()

            // Expanding radar ripple
            drawCircle(
                color = lineColor.copy(alpha = pulseAlpha),
                radius = pulseRadius.dp.toPx(),
                center = lastPoint
            )

            // Inner solid pulse dot
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = lastPoint
            )
            drawCircle(
                color = lineColor,
                radius = 2.dp.toPx(),
                center = lastPoint
            )
        }
    }
}

/**
 * Downsamples raw sample points using sliding bucket averages to remove noise
 * while preserving key physiological peaks and valleys.
 */
private fun downsampleSamples(raw: List<Long>, targetCount: Int): List<Long> {
    if (raw.size <= targetCount) return raw

    val bucketSize = raw.size.toDouble() / targetCount
    val result = mutableListOf<Long>()

    for (i in 0 until targetCount) {
        val startIdx = (i * bucketSize).toInt().coerceIn(0, raw.size - 1)
        val endIdx = ((i + 1) * bucketSize).toInt().coerceIn(startIdx + 1, raw.size)
        val bucket = raw.subList(startIdx, endIdx)
        if (bucket.isNotEmpty()) {
            result.add((bucket.sum() / bucket.size.toDouble()).toLong())
        }
    }
    return result
}
