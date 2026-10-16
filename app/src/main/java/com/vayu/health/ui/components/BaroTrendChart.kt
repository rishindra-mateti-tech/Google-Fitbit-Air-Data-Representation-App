package com.vayu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vayu.health.ui.theme.*

data class TrendPoint(
    val label: String,
    val value: Float
)

/**
 * Baro trend line chart with segmented period picker (W | M | 6M),
 * shaded typical range band, cubic curve, and hollow circular data markers.
 */
@Composable
fun BaroTrendChart(
    points: List<TrendPoint>,
    typicalMin: Float,
    typicalMax: Float,
    accentColor: Color,
    unit: String,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp
) {
    var selectedPeriod by remember { mutableStateOf("W") }

    Column(modifier = modifier.fillMaxWidth()) {
        // Period Segmented Control (W | M | 6M) and Unit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val minStr = if (typicalMin % 1f == 0f) "%.0f".format(typicalMin) else "%.1f".format(typicalMin)
            val maxStr = if (typicalMax % 1f == 0f) "%.0f".format(typicalMax) else "%.1f".format(typicalMax)
            Text(
                text = "Range: $minStr–$maxStr $unit",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = BaroTextMuted
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF18181C))
                    .border(1.dp, BaroBorder, RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                listOf("W", "M", "6M").forEach { period ->
                    val isSelected = period == selectedPeriod
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BaroCardSecondary else Color.Transparent)
                            .clickable { selectedPeriod = period }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BaroTextWhite else BaroTextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Trend Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            if (points.isEmpty()) return@Canvas

            val w = size.width
            val h = size.height

            val allValues = points.map { it.value } + listOf(typicalMin, typicalMax)
            val minVal = (allValues.minOrNull() ?: 0f) * 0.9f
            val maxVal = (allValues.maxOrNull() ?: 100f) * 1.1f
            val range = (maxVal - minVal).coerceAtLeast(1f)

            val padY = 12.dp.toPx()
            val usableH = h - padY * 2
            val stepX = w / (points.size - 1).coerceAtLeast(1)

            // Draw Typical Range Shaded Band
            val bandTopNorm = (typicalMax - minVal) / range
            val bandBottomNorm = (typicalMin - minVal) / range
            val bandTopY = h - padY - (bandTopNorm * usableH)
            val bandBottomY = h - padY - (bandBottomNorm * usableH)

            drawRect(
                color = Color(0x18FFFFFF),
                topLeft = Offset(0f, bandTopY),
                size = androidx.compose.ui.geometry.Size(w, (bandBottomY - bandTopY).coerceAtLeast(2.dp.toPx()))
            )

            // Dashed horizontal grid line
            drawLine(
                color = Color(0x1AFFFFFF),
                start = Offset(0f, bandTopY),
                end = Offset(w, bandTopY),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0x1AFFFFFF),
                start = Offset(0f, bandBottomY),
                end = Offset(w, bandBottomY),
                strokeWidth = 1.dp.toPx()
            )

            // Calculate point coordinates
            val coords = points.mapIndexed { index, pt ->
                val norm = (pt.value - minVal) / range
                val y = h - padY - (norm * usableH)
                val x = index * stepX
                Offset(x, y)
            }

            // Path curve
            val path = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val midX = (p0.x + p1.x) / 2f
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }

            // Fill gradient under curve
            val fillPath = Path().apply {
                addPath(path)
                lineTo(coords.last().x, h)
                lineTo(coords.first().x, h)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.22f), Color.Transparent),
                    startY = padY,
                    endY = h
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = accentColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw hollow circular markers at data points
            coords.forEach { pt ->
                drawCircle(
                    color = Color(0xFF121214),
                    radius = 4.5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = accentColor,
                    radius = 4.5.dp.toPx(),
                    center = pt,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-axis date labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(pt.label, fontSize = 9.sp, color = BaroTextMuted)
            }
        }
    }
}
