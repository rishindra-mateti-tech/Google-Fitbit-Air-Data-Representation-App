package com.vayu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vayu.health.ui.theme.BaroHeartCoral

/**
 * Smooth cubic Bézier sparkline curve for the Baro Heart Rate tile.
 * Renders authentic heart rate samples or a smooth baseline wave with fill.
 */
@Composable
fun BaroSparklineWave(
    samples: List<Long>,
    modifier: Modifier = Modifier,
    lineColor: Color = BaroHeartCoral,
    strokeWidth: Dp = 2.dp,
    showFill: Boolean = true,
    height: Dp = 48.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (samples.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height

        val values = if (samples.size == 1) {
            listOf(samples[0], samples[0])
        } else {
            samples
        }

        val minVal = (values.minOrNull() ?: 60L).toFloat()
        val maxVal = (values.maxOrNull() ?: 120L).toFloat()
        val range = (maxVal - minVal).coerceAtLeast(10f)

        // Map values to points with top/bottom padding
        val padY = 6.dp.toPx()
        val usableH = h - padY * 2
        val stepX = w / (values.size - 1).coerceAtLeast(1)

        val points = values.mapIndexed { index, v ->
            val norm = (v - minVal) / range
            val y = h - padY - (norm * usableH)
            val x = index * stepX
            Offset(x, y)
        }

        // Build smooth cubic Bézier path
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
                        lineColor.copy(alpha = 0.28f),
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
    }
}
