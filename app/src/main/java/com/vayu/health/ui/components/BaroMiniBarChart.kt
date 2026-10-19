package com.vayu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.ui.theme.BaroReadinessTeal
import com.vayu.health.ui.theme.BaroStrainBlue
import com.vayu.health.ui.theme.BaroTextMuted

/**
 * Hourly mini-bar chart for the Baro Steps card.
 * Renders vertical rounded bars across the 24-hour day with time labels (12a, 7a, 1p, etc.).
 */
@Composable
fun BaroHourlyBarChart(
    hourlyValues: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = BaroStrainBlue,
    emptyBarColor: Color = Color(0xFF222226),
    height: Dp = 46.dp
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val totalBars = 24
            val data = if (hourlyValues.isEmpty()) {
                List(totalBars) { 0f }
            } else {
                hourlyValues.take(totalBars).let {
                    if (it.size < totalBars) it + List(totalBars - it.size) { 0f } else it
                }
            }

            val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
            val w = size.width
            val h = size.height

            val barSpacing = 2.dp.toPx()
            val totalSpacing = barSpacing * (totalBars - 1)
            val barWidth = ((w - totalSpacing) / totalBars).coerceAtLeast(1.5f)
            val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

            data.forEachIndexed { i, value ->
                val x = i * (barWidth + barSpacing)
                val barFraction = (value / maxVal).coerceIn(0f, 1f)
                val barH = if (barFraction > 0f) (barFraction * (h - 4.dp.toPx())).coerceAtLeast(4.dp.toPx()) else 3.dp.toPx()
                val y = h - barH

                val isPresent = value > 0f
                drawRoundRect(
                    color = if (isPresent) barColor else emptyBarColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH),
                    cornerRadius = cornerRadius
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("12a", fontSize = 9.sp, color = BaroTextMuted)
            Text("7a", fontSize = 9.sp, color = BaroTextMuted)
            Text("1p", fontSize = 9.sp, color = BaroTextMuted)
            Text("8p", fontSize = 9.sp, color = BaroTextMuted)
        }
    }
}

/**
 * 7-day weekly bar chart for the Baro Zone Minutes card.
 * Renders S S M T W T F columns with rounded pill bars and optional status dots.
 */
@Composable
fun BaroWeeklyBarChart(
    dailyValues: List<Float> = emptyList(),
    dailyNullableValues: List<Float?>? = null,
    labels: List<String>? = null,
    modifier: Modifier = Modifier,
    barColor: Color = BaroReadinessTeal,
    emptyBarColor: Color = Color(0xFF222226),
    todayIndex: Int = 6, // 0 = 6 days ago ... 6 = today
    height: Dp = 46.dp
) {
    val dayLabels = labels ?: listOf("S", "S", "M", "T", "W", "T", "F")

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val count = 7
            val nullableValues: List<Float?> = if (dailyNullableValues != null) {
                if (dailyNullableValues.size < count) {
                    dailyNullableValues + List(count - dailyNullableValues.size) { null }
                } else dailyNullableValues.take(count)
            } else {
                val base = if (dailyValues.isEmpty()) List(count) { 0f } else {
                    if (dailyValues.size < count) dailyValues + List(count - dailyValues.size) { 0f } else dailyValues.take(count)
                }
                base.map { if (it > 0f) it else null }
            }

            val validNumbers = nullableValues.filterNotNull()
            val maxVal = (validNumbers.maxOrNull() ?: 1f).coerceAtLeast(1f)
            val w = size.width
            val h = size.height

            val barSpacing = 10.dp.toPx()
            val totalSpacing = barSpacing * (count - 1)
            val barWidth = ((w - totalSpacing) / count).coerceIn(6.dp.toPx(), 18.dp.toPx())
            val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

            // Centering bars in canvas
            val totalContentWidth = count * barWidth + totalSpacing
            val startOffsetX = (w - totalContentWidth) / 2f

            nullableValues.forEachIndexed { i, value ->
                val x = startOffsetX + i * (barWidth + barSpacing)
                val isRecorded = value != null
                val barFraction = if (value != null) (value / maxVal).coerceIn(0.08f, 1f) else 0f
                val barH = if (isRecorded) (barFraction * (h - 8.dp.toPx())).coerceAtLeast(6.dp.toPx()) else 4.dp.toPx()
                val y = h - barH

                val isToday = i == todayIndex

                drawRoundRect(
                    color = if (isRecorded) barColor else emptyBarColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH),
                    cornerRadius = cornerRadius
                )

                // Dot indicator for today or highlighted days when recorded
                if (isToday && isRecorded) {
                    drawCircle(
                        color = barColor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(x + barWidth / 2f, (y - 5.dp.toPx()).coerceAtLeast(3.dp.toPx()))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayLabels.take(7).forEachIndexed { idx, label ->
                val isToday = idx == todayIndex
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = if (isToday) barColor else BaroTextMuted
                )
            }
        }
    }
}
