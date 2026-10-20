package com.vayu.health.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HistoricalMetricPoint
import com.vayu.health.data.model.HistoricalMetricSeries
import com.vayu.health.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Interactive, high-fidelity multi-range health chart supporting:
 * 1. Touch scrubbing with floating glassmorphic tooltip pill (exact value, date, range status).
 * 2. 30-day / baseline shaded typical range ribbon.
 * 3. Both discrete bar mode (Steps, Calories) and smooth cubic Bézier curve mode (HR, HRV, SpO2, Temp).
 * 4. Zero-hallucination rendering: missing days are rendered cleanly without fake zeros.
 */
@Composable
fun BaroMultiRangeChart(
    series: HistoricalMetricSeries,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isBarChart: Boolean = false,
    height: Dp = 180.dp,
    onPointSelected: ((LocalDate?) -> Unit)? = null
) {
    val points = series.points
    val hasAnyData = points.any { it.value != null } || series.baselineMin != null
    if (points.isEmpty() || !hasAnyData) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No readings recorded in this timeframe", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = BaroTextWhite.copy(alpha = 0.85f))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Wear your tracker regularly to build historical trends.", fontSize = 11.sp, color = BaroTextMuted)
            }
        }
        return
    }

    var hoveredIndex by remember(series) { mutableStateOf<Int?>(null) }
    val selectedPoint = hoveredIndex?.let { points.getOrNull(it) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Floating Scrubber Info Bar / Baseline Summary Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedPoint != null) {
                val dFmt = DateTimeFormatter.ofPattern("EEE, d MMM").format(selectedPoint.date)
                val valStr = selectedPoint.value?.let { v ->
                    if (v % 1.0 == 0.0) "%,.0f %s".format(v, series.unit)
                    else "%.1f %s".format(v, series.unit)
                } ?: "No reading"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dFmt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BaroTextWhite
                    )
                    Text("•", fontSize = 11.sp, color = BaroTextMuted)
                    Text(
                        text = valStr,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedPoint.value != null) accentColor else BaroTextMuted
                    )

                    // Baseline Comparison status
                    val bMin = series.baselineMin
                    val bMax = series.baselineMax
                    val v = selectedPoint.value
                    if (v != null && bMin != null && bMax != null) {
                        val (statusText, statusColor) = when {
                            v < bMin -> "v Lower" to BaroTextMuted
                            v > bMax -> "^ Higher" to Color(0xFFFFA726)
                            else -> "✓ Typical" to BaroReadinessTeal
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(statusText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                }
            } else {
                // Default view: Average + Baseline Range
                val avgStr = series.average?.let {
                    if (it % 1.0 == 0.0) "AVG %,.0f %s".format(it, series.unit)
                    else "AVG %.1f %s".format(it, series.unit)
                } ?: "No data"

                val rangeStr = if (series.baselineMin != null && series.baselineMax != null) {
                    val minS = if (series.baselineMin % 1.0 == 0.0) "%.0f".format(series.baselineMin) else "%.1f".format(series.baselineMin)
                    val maxS = if (series.baselineMax % 1.0 == 0.0) "%.0f".format(series.baselineMax) else "%.1f".format(series.baselineMax)
                    "Typical: $minS–$maxS ${series.unit}"
                } else null

                Text(avgStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                if (rangeStr != null) {
                    Text(rangeStr, fontSize = 10.5.sp, color = BaroTextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Chart Canvas with Touch Gestures
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(points) {
                    detectTapGestures(
                        onPress = { offset ->
                            val step = size.width / points.size.coerceAtLeast(1)
                            val idx = (offset.x / step).toInt().coerceIn(0, points.lastIndex)
                            hoveredIndex = idx
                            onPointSelected?.invoke(points[idx].date)
                        }
                    )
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val step = size.width / points.size.coerceAtLeast(1)
                            val idx = (offset.x / step).toInt().coerceIn(0, points.lastIndex)
                            hoveredIndex = idx
                            onPointSelected?.invoke(points[idx].date)
                        },
                        onDragEnd = {
                            // Keep last hovered or release
                        },
                        onDragCancel = {
                            hoveredIndex = null
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val step = size.width / points.size.coerceAtLeast(1)
                            val idx = (change.position.x / step).toInt().coerceIn(0, points.lastIndex)
                            hoveredIndex = idx
                            onPointSelected?.invoke(points[idx].date)
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val padY = 16.dp.toPx()
            val usableH = h - padY * 2

            val validValues = points.mapNotNull { it.value } + listOfNotNull(series.baselineMin, series.baselineMax)
            if (validValues.isEmpty()) {
                return@Canvas
            }

            val minVal = (validValues.minOrNull() ?: 0.0) * 0.90
            val maxVal = (validValues.maxOrNull() ?: 100.0) * 1.08
            val range = (maxVal - minVal).coerceAtLeast(1.0)

            // Draw Typical Range Shaded Band if available
            val bMin = series.baselineMin
            val bMax = series.baselineMax
            if (bMin != null && bMax != null) {
                val bandTopNorm = ((bMax - minVal) / range).toFloat()
                val bandBottomNorm = ((bMin - minVal) / range).toFloat()
                val bandTopY = h - padY - (bandTopNorm * usableH)
                val bandBottomY = h - padY - (bandBottomNorm * usableH)

                drawRect(
                    color = Color(0x1400D4AA),
                    topLeft = Offset(0f, bandTopY),
                    size = Size(w, (bandBottomY - bandTopY).coerceAtLeast(2.dp.toPx()))
                )

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                drawLine(
                    color = Color(0x3300D4AA),
                    start = Offset(0f, bandTopY),
                    end = Offset(w, bandTopY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = Color(0x3300D4AA),
                    start = Offset(0f, bandBottomY),
                    end = Offset(w, bandBottomY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            val count = points.size
            val slotWidth = w / count.coerceAtLeast(1)

            if (isBarChart) {
                // Discrete Bar Chart Mode (Steps, Calories)
                val barWidth = (slotWidth * 0.65f).coerceAtMost(28.dp.toPx()).coerceAtLeast(3.dp.toPx())
                points.forEachIndexed { i, pt ->
                    val cx = i * slotWidth + slotWidth / 2f
                    val v = pt.value
                    val isHovered = hoveredIndex == i

                    if (v != null && v > 0.0) {
                        val norm = ((v - minVal) / range).toFloat().coerceIn(0.04f, 1f)
                        val barH = norm * usableH
                        val topY = h - padY - barH

                        val barColor = if (isHovered) Color.White else accentColor.copy(alpha = if (hoveredIndex != null) 0.45f else 0.85f)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(cx - barWidth / 2f, topY),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    } else {
                        // Empty/missing day: draw faint bottom notch
                        drawCircle(
                            color = Color(0x22FFFFFF),
                            radius = 2.dp.toPx(),
                            center = Offset(cx, h - padY)
                        )
                    }
                }
            } else {
                // Continuous Line / Wave Mode (HR, HRV, SpO2, Skin Temp)
                val validCoords = mutableListOf<Pair<Int, Offset>>()
                points.forEachIndexed { i, pt ->
                    val cx = if (count > 1) i * (w / (count - 1)) else w / 2f
                    val v = pt.value
                    if (v != null) {
                        val norm = ((v - minVal) / range).toFloat()
                        val cy = h - padY - (norm * usableH)
                        validCoords.add(i to Offset(cx, cy))
                    }
                }

                if (validCoords.isNotEmpty()) {
                    val path = Path()
                    val fillPath = Path()

                    val firstCoord = validCoords.first().second
                    path.moveTo(firstCoord.x, firstCoord.y)
                    fillPath.moveTo(firstCoord.x, h)
                    fillPath.lineTo(firstCoord.x, firstCoord.y)

                    for (k in 0 until validCoords.size - 1) {
                        val p0 = validCoords[k].second
                        val p1 = validCoords[k + 1].second
                        val midX = (p0.x + p1.x) / 2f
                        path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }

                    val lastCoord = validCoords.last().second
                    fillPath.lineTo(lastCoord.x, h)
                    fillPath.close()

                    // Draw gradient fill under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.20f), Color.Transparent),
                            startY = padY,
                            endY = h
                        )
                    )

                    // Draw line curve
                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw data point circles
                    validCoords.forEach { (origIndex, pt) ->
                        val isHovered = hoveredIndex == origIndex
                        val radius = if (isHovered) 6.dp.toPx() else 3.5.dp.toPx()
                        drawCircle(
                            color = Color(0xFF101014),
                            radius = radius,
                            center = pt
                        )
                        drawCircle(
                            color = if (isHovered) Color.White else accentColor,
                            radius = radius,
                            center = pt,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Draw vertical scrubber guide line when hovering
            if (hoveredIndex != null) {
                val hIdx = hoveredIndex!!
                val cursorX = if (isBarChart) {
                    hIdx * slotWidth + slotWidth / 2f
                } else {
                    if (count > 1) hIdx * (w / (count - 1)) else w / 2f
                }
                drawLine(
                    color = Color(0x66FFFFFF),
                    start = Offset(cursorX, 0f),
                    end = Offset(cursorX, h),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-axis Date Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelCount = if (points.size <= 7) points.size else 5
            val step = if (points.size <= 7) 1 else (points.size - 1) / (labelCount - 1)
            (0 until points.size step step.coerceAtLeast(1)).forEach { idx ->
                val p = points.getOrNull(idx)
                if (p != null) {
                    val label = when {
                        points.size <= 7 -> DateTimeFormatter.ofPattern("EEE").format(p.date).take(1)
                        points.size <= 31 -> DateTimeFormatter.ofPattern("d MMM").format(p.date)
                        else -> DateTimeFormatter.ofPattern("MMM").format(p.date)
                    }
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = if (hoveredIndex == idx) FontWeight.Bold else FontWeight.Normal,
                        color = if (hoveredIndex == idx) BaroTextWhite else BaroTextMuted
                    )
                }
            }
        }
    }
}
