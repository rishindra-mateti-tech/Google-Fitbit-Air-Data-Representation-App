package com.vayu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.ui.theme.TextMuted

data class WavePoint(
    val xLabel: String,
    val value: Float
)

@Composable
fun SmoothWaveChart(
    points: List<WavePoint>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 110.dp,
    showGrid: Boolean = true
) {
    if (points.size < 2) return

    val minValue = points.minOf { it.value }
    val maxValue = points.maxOf { it.value }.coerceAtLeast(minValue + 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (points.size - 1)

            // Optional grid lines
            if (showGrid) {
                val gridY1 = height * 0.25f
                val gridY2 = height * 0.75f
                drawLine(
                    color = Color(0x10FFFFFF),
                    start = Offset(0f, gridY1),
                    end = Offset(width, gridY1),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0x10FFFFFF),
                    start = Offset(0f, gridY2),
                    end = Offset(width, gridY2),
                    strokeWidth = 1f
                )
            }

            // Calculate point coordinates
            val coords = points.mapIndexed { index, p ->
                val x = index * stepX
                val normalizedY = (p.value - minValue) / (maxValue - minValue)
                val y = height - (normalizedY * (height * 0.75f) + (height * 0.12f))
                Offset(x, y)
            }

            // Build smooth cubic bezier path
            val linePath = Path()
            val fillPath = Path()

            linePath.moveTo(coords[0].x, coords[0].y)
            fillPath.moveTo(coords[0].x, height)
            fillPath.lineTo(coords[0].x, coords[0].y)

            for (i in 0 until coords.size - 1) {
                val current = coords[i]
                val next = coords[i + 1]
                val controlPoint1 = Offset(current.x + (next.x - current.x) / 2f, current.y)
                val controlPoint2 = Offset(current.x + (next.x - current.x) / 2f, next.y)

                linePath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    next.x, next.y
                )
                fillPath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    next.x, next.y
                )
            }

            fillPath.lineTo(coords.last().x, height)
            fillPath.close()

            // Draw Area Gradient Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.35f),
                        lineColor.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            )

            // Draw Stroke Line
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.7f),
                        lineColor,
                        lineColor.copy(alpha = 0.9f)
                    )
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Highlight latest point with glowing dot
            val latest = coords.last()
            drawCircle(
                color = lineColor.copy(alpha = 0.35f),
                radius = 8.dp.toPx(),
                center = latest
            )
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = latest
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = latest
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Time / X labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { p ->
                Text(
                    text = p.xLabel,
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
