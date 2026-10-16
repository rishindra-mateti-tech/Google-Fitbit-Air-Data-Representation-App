package com.vayu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.SleepStageSegment
import com.vayu.health.data.model.SleepStageType
import com.vayu.health.ui.theme.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Baro 4-lane Hypnogram swim-lane sleep chart.
 * Renders sleep stages along a horizontal timeline:
 * - Lane 0 (Top): Awake (Orange)
 * - Lane 1: REM (Purple)
 * - Lane 2: Light (Light Blue)
 * - Lane 3 (Bottom): Deep (Indigo)
 */
@Composable
fun BaroHypnogramTimeline(
    stages: List<SleepStageSegment>,
    sessionStart: Instant,
    sessionEnd: Instant,
    modifier: Modifier = Modifier,
    height: Dp = 110.dp
) {
    val totalDurationMillis = Duration.between(sessionStart, sessionEnd).toMillis().coerceAtLeast(1)
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("STAGES OF SLEEP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hypnogram Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val w = size.width
            val h = size.height

            val laneCount = 4
            val laneH = h / laneCount
            val corner = CornerRadius(2.dp.toPx(), 2.dp.toPx())

            // Background lane grid lines
            for (i in 0 until laneCount) {
                val y = i * laneH
                drawLine(
                    color = Color(0x12FFFFFF),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw stage segments
            stages.forEach { segment ->
                val startOffsetMillis = Duration.between(sessionStart, segment.startTime).toMillis().coerceAtLeast(0)
                val segmentDurationMillis = Duration.between(segment.startTime, segment.endTime).toMillis().coerceAtLeast(0)

                val xStart = (startOffsetMillis.toFloat() / totalDurationMillis.toFloat()) * w
                val segW = ((segmentDurationMillis.toFloat() / totalDurationMillis.toFloat()) * w).coerceAtLeast(2.dp.toPx())

                val (laneIndex, color) = when (segment.stage) {
                    SleepStageType.AWAKE, SleepStageType.OUT_OF_BED -> Pair(0, SleepAwake)
                    SleepStageType.REM -> Pair(1, SleepRem)
                    SleepStageType.LIGHT, SleepStageType.SLEEPING -> Pair(2, SleepLight)
                    SleepStageType.DEEP -> Pair(3, SleepDeep)
                    SleepStageType.UNKNOWN -> Pair(2, Color.Gray)
                }

                val yTop = laneIndex * laneH + 2.dp.toPx()
                val blockH = laneH - 4.dp.toPx()

                drawRoundRect(
                    color = color,
                    topLeft = Offset(xStart, yTop),
                    size = Size(segW, blockH),
                    cornerRadius = corner
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Time axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(timeFormatter.format(sessionStart), fontSize = 10.sp, color = BaroTextMuted)
            Text(timeFormatter.format(sessionEnd), fontSize = 10.sp, color = BaroTextMuted)
        }
    }
}

/**
 * Horizontal proportional bar showing sleep stage distribution with minutes tags.
 */
@Composable
fun BaroStageBreakdownBar(
    deepMins: Long,
    remMins: Long,
    lightMins: Long,
    awakeMins: Long,
    modifier: Modifier = Modifier
) {
    val total = (deepMins + remMins + lightMins + awakeMins).coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        // Proportional Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            if (awakeMins > 0) {
                Box(Modifier.weight((awakeMins.toFloat() / total).coerceAtLeast(0.01f)).fillMaxHeight().background(SleepAwake))
            }
            if (remMins > 0) {
                Box(Modifier.weight((remMins.toFloat() / total).coerceAtLeast(0.01f)).fillMaxHeight().background(SleepRem))
            }
            if (lightMins > 0) {
                Box(Modifier.weight((lightMins.toFloat() / total).coerceAtLeast(0.01f)).fillMaxHeight().background(SleepLight))
            }
            if (deepMins > 0) {
                Box(Modifier.weight((deepMins.toFloat() / total).coerceAtLeast(0.01f)).fillMaxHeight().background(SleepDeep))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stage tags
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StageTag("Awake", awakeMins, SleepAwake)
            StageTag("REM", remMins, SleepRem)
            StageTag("Light", lightMins, SleepLight)
            StageTag("Deep", deepMins, SleepDeep)
        }
    }
}

@Composable
private fun StageTag(label: String, minutes: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        val h = minutes / 60
        val m = minutes % 60
        val timeStr = if (h > 0) "${h}h ${m}m" else "${m}m"
        Text("$label $timeStr", fontSize = 10.sp, color = BaroTextSecondary)
    }
}
