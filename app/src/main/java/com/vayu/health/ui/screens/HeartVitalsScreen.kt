package com.vayu.health.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

enum class SelectedVitalTab {
    RESTING_HR,
    HRV,
    RESP_RATE,
    SPO2,
    SKIN_TEMP
}

/**
 * Pixel-accurate Baro Vitals & Heart Screen.
 * Implements:
 * 1. "63–151 bpm" hero day range
 * 2. Continuous timeline graph with Zone dashed reference lines
 * 3. Proportional heart rate zone bars (Zone 1-4)
 * 4. Interactive Trend View with W | M | 6M, baseline range band, hollow circular dots
 * 5. Daily measurements list with Baro vertical range pills
 */
@Composable
fun HeartVitalsScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val hr = state.heartRate
    val rhr = state.restingHeartRate
    val hrv = state.hrv
    val resp = state.respiratory

    val minBpm = hr?.minBpm ?: 63L
    val maxBpm = hr?.maxBpm ?: 151L
    val restBpm = rhr?.beatsPerMinute ?: 76L

    var activeVital by remember { mutableStateOf(SelectedVitalTab.RESTING_HR) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BaroBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Stepper Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, null, tint = BaroTextMuted, modifier = Modifier.size(20.dp))
                Text(viewModel.getBaroHeaderDate(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Icon(Icons.Default.ChevronRight, null, tint = BaroTextMuted, modifier = Modifier.size(20.dp))
            }

            val pillText = ProvenanceFormatter.formatCompact(state.selectedSource)
            GlassPill(text = pillText, dotColor = BaroHeartCoral)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Heart Rate Card
        BaroCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 18.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HEART RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                    Text("REST $restBpm BPM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroReadinessTeal)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$minBpm–$maxBpm bpm",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline with Zone Reference Lines
                HeartRateTimelineWithZones(
                    samples = hr?.samples?.map { it.beatsPerMinute } ?: listOf(65L, 72L, 81L, 95L, 120L, 151L, 90L, 78L, 74L),
                    minBpm = minBpm,
                    maxBpm = maxBpm,
                    height = 140.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("12:00 AM", fontSize = 9.sp, color = BaroTextMuted)
                    Text("6:00 AM", fontSize = 9.sp, color = BaroTextMuted)
                    Text("12:00 PM", fontSize = 9.sp, color = BaroTextMuted)
                    Text("6:00 PM", fontSize = 9.sp, color = BaroTextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Heart Rate Zones Breakdown Card
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("TIME IN ZONES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(12.dp))

                ZoneRow("Zone 4 · Peak (>150 bpm)", "5 min", 0.08f, BaroZone4Red)
                Spacer(modifier = Modifier.height(8.dp))
                ZoneRow("Zone 3 · Cardio (125-150 bpm)", "16 min", 0.22f, BaroZone3Orange)
                Spacer(modifier = Modifier.height(8.dp))
                ZoneRow("Zone 2 · Fat Burn (95-125 bpm)", "42 min", 0.45f, BaroZone2Blue)
                Spacer(modifier = Modifier.height(8.dp))
                ZoneRow("Zone 1 · Below / Warm up (<95 bpm)", "6h 15m", 0.85f, BaroZone1Green)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Interactive Trend Chart Section
        val trendTitle = when (activeVital) {
            SelectedVitalTab.RESTING_HR -> "Resting Heart Rate Trend"
            SelectedVitalTab.HRV -> "HRV (RMSSD) Trend"
            SelectedVitalTab.RESP_RATE -> "Respiratory Rate Trend"
            SelectedVitalTab.SPO2 -> "Blood Oxygen (SpO2) Trend"
            SelectedVitalTab.SKIN_TEMP -> "Skin Temperature Variation"
        }

        val trendAccent = when (activeVital) {
            SelectedVitalTab.RESTING_HR -> BaroHeartCoral
            SelectedVitalTab.HRV -> BaroReadinessTeal
            SelectedVitalTab.RESP_RATE -> BaroReadinessTeal
            SelectedVitalTab.SPO2 -> BaroStrainBlue
            SelectedVitalTab.SKIN_TEMP -> SkinTempPurple
        }

        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = trendTitle.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextSecondary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "< Oct 10 - Oct 16 >",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BaroTextWhite
                )

                Spacer(modifier = Modifier.height(14.dp))

                val mockTrendPoints = when (activeVital) {
                    SelectedVitalTab.RESTING_HR -> listOf(
                        TrendPoint("10 Oct", 73f),
                        TrendPoint("11 Oct", 74f),
                        TrendPoint("12 Oct", 75f),
                        TrendPoint("13 Oct", 74f),
                        TrendPoint("14 Oct", 78f),
                        TrendPoint("15 Oct", 77f),
                        TrendPoint("16 Oct", 76f)
                    )
                    SelectedVitalTab.HRV -> listOf(
                        TrendPoint("10 Oct", 29f),
                        TrendPoint("11 Oct", 30f),
                        TrendPoint("12 Oct", 33f),
                        TrendPoint("13 Oct", 32f),
                        TrendPoint("14 Oct", 28f),
                        TrendPoint("15 Oct", 30f),
                        TrendPoint("16 Oct", 31f)
                    )
                    SelectedVitalTab.RESP_RATE -> listOf(
                        TrendPoint("10 Oct", 16.2f),
                        TrendPoint("11 Oct", 15.8f),
                        TrendPoint("12 Oct", 16.0f),
                        TrendPoint("13 Oct", 16.4f),
                        TrendPoint("14 Oct", 15.9f),
                        TrendPoint("15 Oct", 16.1f),
                        TrendPoint("16 Oct", 16.0f)
                    )
                    SelectedVitalTab.SPO2 -> listOf(
                        TrendPoint("10 Oct", 97f),
                        TrendPoint("11 Oct", 96f),
                        TrendPoint("12 Oct", 96f),
                        TrendPoint("13 Oct", 95f),
                        TrendPoint("14 Oct", 96f),
                        TrendPoint("15 Oct", 97f),
                        TrendPoint("16 Oct", 96f)
                    )
                    SelectedVitalTab.SKIN_TEMP -> listOf(
                        TrendPoint("10 Oct", -0.2f),
                        TrendPoint("11 Oct", 0.0f),
                        TrendPoint("12 Oct", +0.1f),
                        TrendPoint("13 Oct", -0.1f),
                        TrendPoint("14 Oct", +0.3f),
                        TrendPoint("15 Oct", 0.0f),
                        TrendPoint("16 Oct", 0.0f)
                    )
                }

                BaroTrendChart(
                    points = mockTrendPoints,
                    typicalMin = when (activeVital) {
                        SelectedVitalTab.RESTING_HR -> 72f
                        SelectedVitalTab.HRV -> 28f
                        SelectedVitalTab.RESP_RATE -> 15.5f
                        SelectedVitalTab.SPO2 -> 95f
                        SelectedVitalTab.SKIN_TEMP -> -0.3f
                    },
                    typicalMax = when (activeVital) {
                        SelectedVitalTab.RESTING_HR -> 77f
                        SelectedVitalTab.HRV -> 35f
                        SelectedVitalTab.RESP_RATE -> 16.5f
                        SelectedVitalTab.SPO2 -> 99f
                        SelectedVitalTab.SKIN_TEMP -> 0.4f
                    },
                    accentColor = trendAccent,
                    unit = when (activeVital) {
                        SelectedVitalTab.RESTING_HR -> "bpm"
                        SelectedVitalTab.HRV -> "ms"
                        SelectedVitalTab.RESP_RATE -> "br/min"
                        SelectedVitalTab.SPO2 -> "%"
                        SelectedVitalTab.SKIN_TEMP -> "°C"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Measurements List
        Text("DAILY MEASUREMENTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MeasurementRow(
                title = "Resting Heart Rate",
                value = "${rhr?.beatsPerMinute ?: 76} bpm",
                status = RangeStatus.HIGHER,
                fraction = 0.72f,
                accent = BaroHeartCoral,
                isSelected = activeVital == SelectedVitalTab.RESTING_HR,
                onClick = { activeVital = SelectedVitalTab.RESTING_HR }
            )

            MeasurementRow(
                title = "Heart Rate Variability (HRV)",
                value = "${hrv?.let { "%.0f".format(it.rmssdMillis) } ?: "31"} ms",
                status = RangeStatus.BUILDING,
                fraction = null,
                accent = BaroReadinessTeal,
                isSelected = activeVital == SelectedVitalTab.HRV,
                onClick = { activeVital = SelectedVitalTab.HRV }
            )

            val respVal = resp?.breathsPerMinute?.let { "%.1f".format(it) } ?: "16.0"
            MeasurementRow(
                title = "Respiratory Rate",
                value = "$respVal br/min",
                status = RangeStatus.LOWER,
                fraction = 0.28f,
                accent = BaroReadinessTeal,
                isSelected = activeVital == SelectedVitalTab.RESP_RATE,
                onClick = { activeVital = SelectedVitalTab.RESP_RATE }
            )

            val spo2Val = resp?.spo2Percentage?.let { "%.0f".format(it) } ?: "96"
            MeasurementRow(
                title = "Oxygen Saturation (SpO2)",
                value = "$spo2Val %",
                status = RangeStatus.TYPICAL,
                fraction = 0.50f,
                accent = BaroStrainBlue,
                isSelected = activeVital == SelectedVitalTab.SPO2,
                onClick = { activeVital = SelectedVitalTab.SPO2 }
            )

            val skinDelta = resp?.skinTemperature?.averageDeltaCelsius?.let { "%+.1f".format(it) } ?: "0.0"
            MeasurementRow(
                title = "Skin Temperature Variation",
                value = "$skinDelta °C",
                status = RangeStatus.BUILDING,
                fraction = null,
                accent = SkinTempPurple,
                isSelected = activeVital == SelectedVitalTab.SKIN_TEMP,
                onClick = { activeVital = SelectedVitalTab.SKIN_TEMP }
            )
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun HeartRateTimelineWithZones(
    samples: List<Long>,
    minBpm: Long,
    maxBpm: Long,
    height: androidx.compose.ui.unit.Dp
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height

        val minVal = minBpm.toFloat().coerceAtMost(50f)
        val maxVal = maxBpm.toFloat().coerceAtLeast(160f)
        val range = maxVal - minVal

        // Dashed lines for heart rate zones
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        // Zone 4: 150 bpm line (Red)
        val z4Y = h - ((150f - minVal) / range) * h
        drawLine(
            color = BaroZone4Red.copy(alpha = 0.4f),
            start = Offset(0f, z4Y),
            end = Offset(w, z4Y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )

        // Zone 2: 125 bpm line (Blue)
        val z2Y = h - ((125f - minVal) / range) * h
        drawLine(
            color = BaroZone2Blue.copy(alpha = 0.4f),
            start = Offset(0f, z2Y),
            end = Offset(w, z2Y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )

        // Resting: 75 bpm line (Teal)
        val rhrY = h - ((75f - minVal) / range) * h
        drawLine(
            color = BaroReadinessTeal.copy(alpha = 0.4f),
            start = Offset(0f, rhrY),
            end = Offset(w, rhrY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )

        // Sample curve
        if (samples.size > 1) {
            val stepX = w / (samples.size - 1)
            val points = samples.mapIndexed { index, bpm ->
                val norm = (bpm - minVal) / range
                val y = h - (norm * h)
                val x = index * stepX
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val midX = (p0.x + p1.x) / 2f
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }

            drawPath(
                path = path,
                color = BaroHeartCoral,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun ZoneRow(name: String, duration: String, fraction: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 11.sp, color = BaroTextSecondary)
            Text(duration, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF222226))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(fraction.coerceIn(0.01f, 1f))
                    .background(color)
            )
            if (fraction < 1f) {
                Spacer(modifier = Modifier.weight(1f - fraction))
            }
        }
    }
}

@Composable
private fun MeasurementRow(
    title: String,
    value: String,
    status: RangeStatus,
    fraction: Float?,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    BaroCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isSelected) accent.copy(alpha = 0.5f) else BaroBorder,
        padding = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(4.dp))
                BaroVerticalRangePill(
                    currentValueFraction = fraction,
                    status = status,
                    accentColor = accent,
                    pillHeight = 24.dp
                )
            }
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
        }
    }
}
